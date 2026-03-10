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
import com.trackspace.user.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Map<String, Object> syncCommits(SyncRequest request) {
        log.info("Starting commit sync for project {}", request.getProjectId());

        // Get ALL connections for this project (multi-repo: FE + BE)
        List<Connection> connections = connectionRepository.findByProjectId(request.getProjectId());
        if (connections.isEmpty()) {
            throw new ResourceNotFoundException(
                    "GitHub connection not found for project: " + request.getProjectId());
        }

        int totalSynced = 0;
        int totalSkipped = 0;

        for (Connection connection : connections) {
            if (connection.getStatus() != com.trackspace.github.ConnectionStatus.CONNECTED) {
                continue; // Skip disconnected repos
            }

            // Parse owner and repo
            String[] ownerRepo = parseRepositoryUrl(connection.getRepositoryUrl());
            String owner = ownerRepo[0];
            String repo = ownerRepo[1];

            // Pre-load all existing commit SHAs for this project (avoids N individual DB
            // queries)
            Set<String> existingShas = new java.util.HashSet<>(
                    commitRepository.findAllShasByConnectionId(connection.getId()));
            log.info("Pre-loaded {} existing SHAs for connection {}", existingShas.size(), connection.getId());

            // Determine since timestamp
            Instant since = request.getSince();
            if (since == null) {
                // If DB has no commits (e.g. deleted), force full sync regardless of
                // lastSyncAt
                if (existingShas.isEmpty()) {
                    since = Instant.now().minus(Duration.ofDays(365));
                    log.info("No commits in DB for project {} — forcing full sync (365 days)",
                            connection.getProjectId());
                } else {
                    since = connection.getLastSyncAt() != null
                            ? connection.getLastSyncAt()
                            : Instant.now().minus(Duration.ofDays(365));
                }
            }

            // Determine branches to sync (all branches if not specified)
            List<String> branchesToSync = new java.util.ArrayList<>();
            if (request.getBranch() != null) {
                branchesToSync.add(request.getBranch());
            } else {
                // Fetch ALL branches from GitHub
                List<GitHubApiClient.GitHubBranchDto> allBranches = gitHubApiClient
                        .fetchBranches(owner, repo, connection.getAccessTokenEncrypted());
                if (allBranches.isEmpty()) {
                    // Fallback to default branch
                    branchesToSync.add(connection.getBranchName());
                } else {
                    // IMPORTANT: Sync default branch FIRST so shared commits get correct branchName
                    String defaultBranch = connection.getBranchName() != null ? connection.getBranchName() : "main";
                    branchesToSync.add(defaultBranch);
                    for (GitHubApiClient.GitHubBranchDto b : allBranches) {
                        if (!b.getName().equals(defaultBranch)) {
                            branchesToSync.add(b.getName());
                        }
                    }
                }
                log.info("Syncing {} branches for repo {}/{} (default: {})",
                        branchesToSync.size(), owner, repo, branchesToSync.get(0));
            }

            // Fetch and save commits for each branch
            for (String branch : branchesToSync) {
                List<GitHubCommitDto> githubCommits = gitHubApiClient
                        .fetchCommits(owner, repo, connection.getAccessTokenEncrypted(), since, branch);

                if (githubCommits == null || githubCommits.isEmpty()) {
                    log.debug("No new commits on branch {} for repo {}/{}", branch, owner, repo);
                    continue;
                }

                // Process and save only NEW commits
                for (GitHubCommitDto githubCommit : githubCommits) {
                    // Skip if already exists (O(1) check — no DB call)
                    if (existingShas.contains(githubCommit.getSha())) {
                        totalSkipped++;
                        continue;
                    }

                    try {
                        // Fetch detailed stats (lines added/deleted, files changed)
                        // List API does NOT return stats — only detail endpoint does
                        GitHubApiClient.GitHubCommitDetailDto detail = gitHubApiClient
                                .fetchCommitDetails(owner, repo, githubCommit.getSha(),
                                        connection.getAccessTokenEncrypted());
                        Commit commit = mapToEntity(githubCommit, detail, connection.getProjectId(), connection.getId(),
                                branch);

                        // Save commit
                        commitRepository.save(commit);
                        existingShas.add(githubCommit.getSha()); // Prevent duplicates across branches
                        totalSynced++;
                    } catch (Exception e) {
                        log.warn("Failed to save commit {}: {}", githubCommit.getSha(), e.getMessage());
                        totalSkipped++;
                    }
                }
            } // end branch loop

            // Update lastSyncAt
            connection.setLastSyncAt(Instant.now());
            connectionRepository.save(connection);

            log.info("Synced repo {}/{}: {} commits", owner, repo, totalSynced);
        }

        log.info("Commit sync completed for project {}: {} synced, {} skipped",
                request.getProjectId(), totalSynced, totalSkipped);

        // Backfill authorId for any existing commits that still have null authorId
        int backfilled = backfillAuthorIds(request.getProjectId());

        return Map.of(
                "commitsSynced", totalSynced,
                "commitsSkipped", totalSkipped,
                "authorIdBackfilled", backfilled,
                "lastSyncAt", Instant.now(),
                "message",
                String.format("Successfully synced %d commits from %d repos", totalSynced, connections.size()));
    }

    @Override
    @Transactional
    public Map<String, Object> syncSingleConnection(Integer connectionId) {
        log.info("[Webhook] Starting single-connection sync for connectionId={}", connectionId);

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + connectionId));

        if (connection.getStatus() != com.trackspace.github.ConnectionStatus.CONNECTED) {
            return Map.of("commitsSynced", 0, "message", "Connection is not CONNECTED");
        }

        String[] ownerRepo = parseRepositoryUrl(connection.getRepositoryUrl());
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];

        Set<String> existingShas = new java.util.HashSet<>(
                commitRepository.findAllShasByConnectionId(connection.getId()));

        // Sync only from lastSyncAt (or 30 days back if never synced)
        Instant since = connection.getLastSyncAt() != null
                ? connection.getLastSyncAt()
                : Instant.now().minus(Duration.ofDays(30));

        // Fetch all branches
        List<GitHubApiClient.GitHubBranchDto> allBranches = gitHubApiClient
                .fetchBranches(owner, repo, connection.getAccessTokenEncrypted());
        List<String> branchesToSync = new java.util.ArrayList<>();
        String defaultBranch = connection.getBranchName() != null ? connection.getBranchName() : "main";
        branchesToSync.add(defaultBranch);
        for (GitHubApiClient.GitHubBranchDto b : allBranches) {
            if (!b.getName().equals(defaultBranch))
                branchesToSync.add(b.getName());
        }

        int synced = 0;
        int skipped = 0;
        for (String branch : branchesToSync) {
            List<GitHubCommitDto> githubCommits = gitHubApiClient
                    .fetchCommits(owner, repo, connection.getAccessTokenEncrypted(), since, branch);
            if (githubCommits == null)
                continue;
            for (GitHubCommitDto ghCommit : githubCommits) {
                if (existingShas.contains(ghCommit.getSha())) {
                    skipped++;
                    continue;
                }
                try {
                    GitHubApiClient.GitHubCommitDetailDto detail = gitHubApiClient
                            .fetchCommitDetails(owner, repo, ghCommit.getSha(), connection.getAccessTokenEncrypted());
                    commitRepository
                            .save(mapToEntity(ghCommit, detail, connection.getProjectId(), connection.getId(), branch));
                    existingShas.add(ghCommit.getSha());
                    synced++;
                } catch (Exception e) {
                    log.warn("[Webhook] Failed to save commit {}: {}", ghCommit.getSha(), e.getMessage());
                    skipped++;
                }
            }
        }

        connection.setLastSyncAt(Instant.now());
        connectionRepository.save(connection);

        log.info("[Webhook] Single-connection sync done for {}/{}: {} new commits", owner, repo, synced);
        return Map.of("commitsSynced", synced, "commitsSkipped", skipped, "lastSyncAt", Instant.now(),
                "message", String.format("Webhook sync: %d new commits for %s/%s", synced, owner, repo));
    }

    @Override
    public List<CommitResponse> getCommits(Integer projectId, Integer connectionId, Integer userId, Instant since,
            Instant until,
            String branch) {
        log.debug("Getting commits for project {}, connection {}, user {}, branch {}", projectId, connectionId, userId,
                branch);

        List<Commit> commits;

        if (connectionId != null) {
            // ── Connection-scoped queries (per-repo) ──
            if (branch != null && !branch.isBlank()) {
                commits = (since != null && until != null)
                        ? commitRepository.findByConnectionIdAndBranchAndDateRange(connectionId, branch, since, until)
                        : commitRepository.findByConnectionIdAndBranch(connectionId, branch);
            } else if (since != null && until != null) {
                commits = commitRepository.findByConnectionIdAndDateRange(connectionId, since, until);
            } else if (userId != null) {
                commits = commitRepository.findByConnectionIdAndAuthorId(connectionId, userId);
            } else {
                commits = commitRepository.findByConnectionIdOrderByCommitDateDesc(connectionId);
            }
            // Apply user filter in-memory if needed
            if (userId != null && branch != null) {
                commits = commits.stream().filter(c -> userId.equals(c.getAuthorId())).collect(Collectors.toList());
            }
        } else {
            // ── Project-wide queries (legacy / all repos) ──
            if (branch != null && !branch.isBlank()) {
                commits = (since != null && until != null)
                        ? commitRepository.findByProjectIdAndBranchContainingAndDateRange(projectId, branch, since,
                                until)
                        : commitRepository.findByProjectIdAndBranchContaining(projectId, branch);
                if (userId != null) {
                    commits = commits.stream().filter(c -> userId.equals(c.getAuthorId())).collect(Collectors.toList());
                }
            } else if (userId != null && since != null && until != null) {
                commits = commitRepository.findByProjectIdAndDateRange(projectId, since, until)
                        .stream().filter(c -> userId.equals(c.getAuthorId())).collect(Collectors.toList());
            } else if (userId != null) {
                commits = commitRepository.findByProjectIdAndAuthorId(projectId, userId);
            } else if (since != null && until != null) {
                commits = commitRepository.findByProjectIdAndDateRange(projectId, since, until);
            } else {
                commits = commitRepository.findByProjectIdOrderByCommitDateDesc(projectId);
            }
        }

        return commits.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StatsResponse> getStats(Integer projectId, Integer connectionId, Integer userId) {
        log.debug("Calculating stats for project {}, connection {}, user {}", projectId, connectionId, userId);

        if (userId != null) {
            return List.of(calculateUserStats(projectId, userId));
        } else {
            return calculateMergedStats(projectId, connectionId);
        }
    }

    /**
     * Smart author merging: same person may use different name/email combos.
     * Strategy:
     * 1. Extract GitHub username from noreply emails (e.g.
     * 12345+TuanHoang297@users.noreply.github.com)
     * 2. Build identity groups using Union-Find:
     * - commits with same email → same person
     * - if noreply email contains username X and another commit has authorName == X
     * → same person
     */
    private List<StatsResponse> calculateMergedStats(Integer projectId, Integer connectionId) {
        List<Commit> rawCommits = connectionId != null
                ? commitRepository.findByConnectionIdOrderByCommitDateDesc(connectionId)
                : commitRepository.findByProjectIdOrderByCommitDateDesc(projectId);
        if (rawCommits.isEmpty())
            return List.of();

        // Exclude merge commits (GitHub doesn't count them in contributions)
        List<Commit> nonMergeCommits = rawCommits.stream()
                .filter(c -> {
                    String msg = c.getCommitMessage();
                    if (msg == null)
                        return true;
                    String lower = msg.toLowerCase().trim();
                    return !lower.startsWith("merge pull request")
                            && !lower.startsWith("merge branch")
                            && !lower.startsWith("merge remote");
                })
                .collect(Collectors.toList());

        // Group by githubLogin (the actual GitHub account — 100% reliable)
        // Use ALL commits to collect names, NON-MERGE for stats
        Map<String, Set<String>> loginToNames = new java.util.LinkedHashMap<>();
        for (Commit c : rawCommits) {
            String login = c.getGithubLogin() != null ? c.getGithubLogin().toLowerCase()
                    : (c.getAuthorEmail() != null ? c.getAuthorEmail().toLowerCase() : "unknown");
            String name = c.getAuthorName() != null ? c.getAuthorName().trim() : "Unknown";
            loginToNames.computeIfAbsent(login, k -> new java.util.HashSet<>()).add(name);
        }

        Map<String, List<Commit>> groups = new java.util.LinkedHashMap<>();
        for (Commit c : nonMergeCommits) {
            String login = c.getGithubLogin() != null ? c.getGithubLogin().toLowerCase()
                    : (c.getAuthorEmail() != null ? c.getAuthorEmail().toLowerCase() : "unknown");
            groups.computeIfAbsent(login, k -> new java.util.ArrayList<>()).add(c);
        }

        // Build stats per GitHub account
        List<StatsResponse> result = new java.util.ArrayList<>();
        for (Map.Entry<String, List<Commit>> entry : groups.entrySet()) {
            List<Commit> groupCommits = entry.getValue();
            Set<String> names = loginToNames.getOrDefault(entry.getKey(), Set.of());

            // Prefer full name (contains space) over username
            String displayName = names.stream()
                    .filter(n -> n.contains(" "))
                    .max(java.util.Comparator.comparingInt(String::length))
                    .orElse(names.stream()
                            .max(java.util.Comparator.comparingInt(String::length))
                            .orElse("Unknown"));

            long totalAdded = groupCommits.stream().mapToLong(c -> c.getLinesAdded() != null ? c.getLinesAdded() : 0)
                    .sum();
            long totalDeleted = groupCommits.stream()
                    .mapToLong(c -> c.getLinesDeleted() != null ? c.getLinesDeleted() : 0).sum();
            Instant lastCommit = groupCommits.stream().map(Commit::getCommitDate).filter(d -> d != null)
                    .max(Instant::compareTo).orElse(null);

            result.add(StatsResponse.builder()
                    .userId(null)
                    .userName(displayName)
                    .githubLogin(entry.getKey())
                    .totalCommits((long) groupCommits.size())
                    .totalLinesAdded(totalAdded)
                    .totalLinesDeleted(totalDeleted)
                    .totalChanges(totalAdded + totalDeleted)
                    .lastCommitAt(lastCommit)
                    .build());
        }

        // Sort by lines added desc (code contribution)
        result.sort((a, b) -> Long.compare(b.getTotalLinesAdded(), a.getTotalLinesAdded()));
        return result;
    }

    /**
     * Map GitHub commit DTO to Commit entity
     */
    private Commit mapToEntity(GitHubCommitDto githubCommit,
            GitHubApiClient.GitHubCommitDetailDto detail,
            Integer projectId,
            Integer connectionId,
            String branch) {
        Commit commit = new Commit();

        commit.setProjectId(projectId);
        commit.setConnectionId(connectionId);
        commit.setCommitSha(githubCommit.getSha());
        commit.setCommitMessage(githubCommit.getCommit().getMessage());

        // Author info
        String authorName = githubCommit.getCommit().getAuthor().getName();
        String authorEmail = githubCommit.getCommit().getAuthor().getEmail();
        commit.setAuthorName(authorName);
        commit.setAuthorEmail(authorEmail);

        // GitHub login (from top-level author object, the actual GitHub account)
        if (githubCommit.getAuthor() != null && githubCommit.getAuthor().getLogin() != null) {
            commit.setGithubLogin(githubCommit.getAuthor().getLogin());
        }

        // Map author email to user ID
        commit.setAuthorId(findUserIdByEmail(authorEmail));

        // Commit date
        String dateStr = githubCommit.getCommit().getAuthor().getDate();
        commit.setCommitDate(Instant.parse(dateStr));

        // Stats from detail API (list API does NOT return stats)
        if (detail != null && detail.getStats() != null) {
            commit.setLinesAdded(detail.getStats().getAdditions() != null ? detail.getStats().getAdditions() : 0);
            commit.setLinesDeleted(detail.getStats().getDeletions() != null ? detail.getStats().getDeletions() : 0);
            commit.setFilesChanged(detail.getFiles() != null ? detail.getFiles().size() : 0);
        } else {
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
                .githubLogin(commit.getGithubLogin())
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
     * Parse repository URL to extract owner and repo
     */
    private String[] parseRepositoryUrl(String url) {
        String cleanUrl = url.replace(".git", "");
        String path = cleanUrl.replace("https://github.com/", "");
        String[] parts = path.split("/");
        return new String[] { parts[0], parts[1] };
    }

    /**
     * Backfill authorId for commits in a project that currently have authorId = null.
     * This patches historical data synced before the email→userId mapping was implemented.
     */
    private int backfillAuthorIds(Integer projectId) {
        List<Commit> unmatched = commitRepository.findByProjectIdAndAuthorIdIsNull(projectId);
        int patched = 0;
        for (Commit commit : unmatched) {
            Integer userId = findUserIdByEmail(commit.getAuthorEmail());
            if (userId != null) {
                commit.setAuthorId(userId);
                commitRepository.save(commit);
                patched++;
            }
        }
        if (patched > 0) {
            log.info("Backfilled authorId for {} / {} commits in project {}", patched, unmatched.size(), projectId);
        }
        return patched;
    }

    /**
     * Map commit author email (or noreply GitHub email) to local user ID.
     * Strategy:
     *   1. Direct email match against User.email
     *   2. Extract GitHub login from noreply format and match against User.githubLogin
     */
    private Integer findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) return null;

        // 1. Direct email match
        Optional<Integer> byEmail = userRepository.findByEmail(email)
                .map(u -> u.getId().intValue());
        if (byEmail.isPresent()) return byEmail.get();

        // 2. GitHub noreply format: "12345+username@users.noreply.github.com"
        if (email.endsWith("@users.noreply.github.com")) {
            String local = email.replace("@users.noreply.github.com", "");
            // Strip numeric prefix (e.g. "12345+username" → "username")
            int plusIdx = local.indexOf('+');
            String login = plusIdx >= 0 ? local.substring(plusIdx + 1) : local;
            return userRepository.findByGithubLogin(login)
                    .map(u -> u.getId().intValue())
                    .orElse(null);
        }

        return null;
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
