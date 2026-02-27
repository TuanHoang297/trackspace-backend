package com.trackspace.github.service.impl;

import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.github.dto.CommitResponse;
import com.trackspace.github.dto.StatsResponse;
import com.trackspace.github.dto.SyncRequest;
import com.trackspace.github.entity.Commit;
import com.trackspace.github.entity.Connection;
import com.trackspace.github.repository.CommitRepository;
import com.trackspace.github.repository.ConnectionRepository;
import com.trackspace.github.service.CommitService;
import com.trackspace.github.service.GitHubApiClient;
import com.trackspace.github.service.GitHubApiClient.GitHubCommitDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of CommitService
 * Handles syncing and retrieving GitHub commits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommitServiceImpl implements CommitService {

    private final CommitRepository commitRepository;
    private final ConnectionRepository connectionRepository;
    private final GitHubApiClient gitHubApiClient;

    // TODO: Inject UserRepository when available
    // private final UserRepository userRepository;

    @Override
    @Transactional
    public Map<String, Object> syncCommits(SyncRequest request) {
        log.info("Starting commit sync for project {}", request.getProjectId());

        // Get connection
        Connection connection = connectionRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "GitHub connection not found for project: " + request.getProjectId()));

        // Parse owner and repo
        String[] ownerRepo = parseRepositoryUrl(connection.getRepositoryUrl());
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];

        // Determine since timestamp
        Instant since = request.getSince();
        if (since == null) {
            // Use lastSyncAt or 30 days ago
            since = connection.getLastSyncAt() != null
                    ? connection.getLastSyncAt()
                    : Instant.now().minus(Duration.ofDays(30));
        }

        // Determine branch
        String branch = request.getBranch() != null
                ? request.getBranch()
                : connection.getBranchName();

        // Fetch commits from GitHub
        List<GitHubCommitDto> githubCommits = gitHubApiClient
                .fetchCommits(owner, repo, connection.getAccessTokenEncrypted(), since, branch);

        if (githubCommits == null || githubCommits.isEmpty()) {
            log.info("No new commits found for project {}", request.getProjectId());
            return Map.of(
                    "commitsSynced", 0,
                    "lastSyncAt", Instant.now(),
                    "message", "No new commits found");
        }

        // Process and save commits
        int syncedCount = 0;
        int skippedCount = 0;

        for (GitHubCommitDto githubCommit : githubCommits) {
            // Check if commit already exists
            if (commitRepository.existsByCommitSha(githubCommit.getSha())) {
                skippedCount++;
                continue;
            }

            // Get detailed commit info (for stats)
            GitHubApiClient.GitHubCommitDetailDto detail = gitHubApiClient
                    .fetchCommitDetails(owner, repo, githubCommit.getSha(), connection.getAccessTokenEncrypted());

            // Map to entity
            Commit commit = mapToEntity(githubCommit, detail, connection.getProjectId(), branch);

            // Save commit
            commitRepository.save(commit);
            syncedCount++;
        }

        // Update lastSyncAt
        connection.setLastSyncAt(Instant.now());
        connectionRepository.save(connection);

        log.info("Commit sync completed for project {}: {} synced, {} skipped",
                request.getProjectId(), syncedCount, skippedCount);

        return Map.of(
                "commitsSynced", syncedCount,
                "commitsSkipped", skippedCount,
                "lastSyncAt", connection.getLastSyncAt(),
                "message", String.format("Successfully synced %d commits", syncedCount));
    }

    @Override
    public List<CommitResponse> getCommits(Integer projectId, Integer userId, Instant since, Instant until) {
        log.debug("Getting commits for project {}, user {}", projectId, userId);

        List<Commit> commits;

        if (userId != null && since != null && until != null) {
            // Filter by user and date range
            commits = commitRepository.findByProjectIdAndDateRange(projectId, since, until)
                    .stream()
                    .filter(c -> userId.equals(c.getAuthorId()))
                    .collect(Collectors.toList());
        } else if (userId != null) {
            // Filter by user only
            commits = commitRepository.findByProjectIdAndAuthorId(projectId, userId);
        } else if (since != null && until != null) {
            // Filter by date range only
            commits = commitRepository.findByProjectIdAndDateRange(projectId, since, until);
        } else {
            // Get all commits for project
            commits = commitRepository.findByProjectIdOrderByCommitDateDesc(projectId);
        }

        return commits.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StatsResponse> getStats(Integer projectId, Integer userId) {
        log.debug("Calculating stats for project {}, user {}", projectId, userId);

        if (userId != null) {
            // Get stats for specific user
            return List.of(calculateUserStats(projectId, userId));
        } else {
            // Get stats for all users
            List<Object[]> results = commitRepository.getContributionStatsByProject(projectId);

            return results.stream()
                    .map(this::mapToStatsResponse)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Map GitHub commit DTO to Commit entity
     */
    private Commit mapToEntity(GitHubCommitDto githubCommit,
            GitHubApiClient.GitHubCommitDetailDto detail,
            Integer projectId,
            String branch) {
        Commit commit = new Commit();

        commit.setProjectId(projectId);
        commit.setCommitSha(githubCommit.getSha());
        commit.setCommitMessage(githubCommit.getCommit().getMessage());

        // Author info
        String authorName = githubCommit.getCommit().getAuthor().getName();
        String authorEmail = githubCommit.getCommit().getAuthor().getEmail();
        commit.setAuthorName(authorName);
        commit.setAuthorEmail(authorEmail);

        // Map author email to user ID
        commit.setAuthorId(findUserIdByEmail(authorEmail));

        // Commit date
        String dateStr = githubCommit.getCommit().getAuthor().getDate();
        commit.setCommitDate(Instant.parse(dateStr));

        // Stats from detail
        if (detail != null && detail.getStats() != null) {
            commit.setLinesAdded(detail.getStats().getAdditions());
            commit.setLinesDeleted(detail.getStats().getDeletions());
            commit.setFilesChanged(detail.getFiles() != null ? detail.getFiles().size() : 0);
        } else {
            // Fallback to basic stats
            commit.setLinesAdded(0);
            commit.setLinesDeleted(0);
            commit.setFilesChanged(0);
        }

        commit.setBranchName(branch);

        // Extract Jira issue from commit message
        commit.setLinkedIssueId(extractJiraIssueId(githubCommit.getCommit().getMessage()));

        return commit;
    }

    /**
     * Map Commit entity to CommitResponse DTO
     */
    private CommitResponse mapToResponse(Commit commit) {
        return CommitResponse.builder()
                .commitId(commit.getId())
                .commitSha(commit.getCommitSha())
                .commitMessage(commit.getCommitMessage())
                .authorName(commit.getAuthorName())
                .authorEmail(commit.getAuthorEmail())
                .authorId(commit.getAuthorId())
                .commitDate(commit.getCommitDate())
                .filesChanged(commit.getFilesChanged())
                .linesAdded(commit.getLinesAdded())
                .linesDeleted(commit.getLinesDeleted())
                .branchName(commit.getBranchName())
                .linkedIssueId(commit.getLinkedIssueId())
                .build();
    }

    /**
     * Calculate stats for a specific user
     */
    private StatsResponse calculateUserStats(Integer projectId, Integer userId) {
        List<Commit> commits = commitRepository.findByProjectIdAndAuthorId(projectId, userId);

        if (commits.isEmpty()) {
            return StatsResponse.builder()
                    .userId(userId)
                    .userName("Unknown")
                    .totalCommits(0L)
                    .totalLinesAdded(0L)
                    .totalLinesDeleted(0L)
                    .totalChanges(0L)
                    .lastCommitAt(null)
                    .build();
        }

        long totalAdded = commits.stream()
                .mapToLong(c -> c.getLinesAdded() != null ? c.getLinesAdded() : 0)
                .sum();

        long totalDeleted = commits.stream()
                .mapToLong(c -> c.getLinesDeleted() != null ? c.getLinesDeleted() : 0)
                .sum();

        Instant lastCommit = commits.stream()
                .map(Commit::getCommitDate)
                .max(Instant::compareTo)
                .orElse(null);

        // TODO: Get user name from UserRepository
        String userName = commits.get(0).getAuthorName();

        return StatsResponse.builder()
                .userId(userId)
                .userName(userName)
                .totalCommits((long) commits.size())
                .totalLinesAdded(totalAdded)
                .totalLinesDeleted(totalDeleted)
                .totalChanges(totalAdded + totalDeleted)
                .lastCommitAt(lastCommit)
                .build();
    }

    /**
     * Map query result to StatsResponse
     */
    private StatsResponse mapToStatsResponse(Object[] result) {
        Integer authorId = (Integer) result[0];
        Long count = (Long) result[1];
        Long additions = (Long) result[2];
        Long deletions = (Long) result[3];

        // TODO: Get user name from UserRepository
        String userName = "User " + authorId;

        // Get last commit date
        Instant lastCommit = commitRepository.findByProjectIdAndAuthorId(
                commitRepository.findByProjectId((Integer) result[0]).get(0).getProjectId(),
                authorId).stream()
                .map(Commit::getCommitDate)
                .max(Instant::compareTo)
                .orElse(null);

        return StatsResponse.builder()
                .userId(authorId)
                .userName(userName)
                .totalCommits(count)
                .totalLinesAdded(additions != null ? additions : 0L)
                .totalLinesDeleted(deletions != null ? deletions : 0L)
                .totalChanges((additions != null ? additions : 0L) + (deletions != null ? deletions : 0L))
                .lastCommitAt(lastCommit)
                .build();
    }

    /**
     * Parse repository URL to extract owner and repo
     */
    private String[] parseRepositoryUrl(String url) {
        String cleanUrl = url.replace(".git", "");
        String path = cleanUrl.replace("https://github.com/", "");
        String[] parts = path.split("/");
        return new String[] { parts[0], parts[1] };
    }

    /**
     * Find user ID by email
     * TODO: Implement actual user lookup when UserRepository is available
     */
    private Integer findUserIdByEmail(String email) {
        // TODO: Implement actual lookup
        // Optional<User> user = userRepository.findByEmail(email);
        // return user.map(User::getId).orElse(null);

        log.debug("User mapping not implemented yet for email: {}", email);
        return null; // Will be null until user mapping is implemented
    }

    /**
     * Extract Jira issue ID from commit message
     * Pattern: Looks for JIRA-123, SRS-456, etc.
     */
    private Integer extractJiraIssueId(String commitMessage) {
        if (commitMessage == null) {
            return null;
        }

        // Pattern to match Jira issue keys (e.g., SRS-123, JIRA-456)
        Pattern pattern = Pattern.compile("([A-Z]+-\\d+)");
        Matcher matcher = pattern.matcher(commitMessage);

        if (matcher.find()) {
            String issueKey = matcher.group(1);
            log.debug("Found Jira issue key in commit message: {}", issueKey);

            // TODO: Look up issue ID from jira_issues table by issue_key
            // Optional<JiraIssue> issue = jiraIssueRepository.findByIssueKey(issueKey);
            // return issue.map(JiraIssue::getId).orElse(null);

            return null; // Will be implemented when Jira integration is available
        }

        return null;
    }
}
