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
import com.trackspace.analytics.MetricsCalculator;
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

            // Always fetch full history (365 days) and rely on SHA deduplication.
            // This ensures any commits missed by previous non-paginated syncs are captured.
            Instant since = request.getSince();
            if (since == null) {
                since = Instant.now().minus(Duration.ofDays(365));
                log.info("Full sync (365 days) for project {}, existing SHAs: {}",
                        connection.getProjectId(), existingShas.size());
            }

            // Only sync DEFAULT branch for speed — other branches use real-time API (getCommitsByBranch)
            List<String> branchesToSync = new java.util.ArrayList<>();
            if (request.getBranch() != null) {
                branchesToSync.add(request.getBranch());
            } else {
                String defaultBranch = connection.getBranchName() != null ? connection.getBranchName() : "main";
                branchesToSync.add(defaultBranch);
                log.info("Syncing default branch '{}' for repo {}/{} (other branches via real-time API)",
                        defaultBranch, owner, repo);
            }

            // Fetch and save commits INSTANTLY (no detail fetch — stats loaded in background)
            List<Commit> savedCommits = new java.util.ArrayList<>();
            for (String branch : branchesToSync) {
                List<GitHubCommitDto> githubCommits = gitHubApiClient
                        .fetchCommits(owner, repo, connection.getAccessTokenEncrypted(), since, branch);

                if (githubCommits == null || githubCommits.isEmpty()) continue;

                for (GitHubCommitDto ghCommit : githubCommits) {
                    if (existingShas.contains(ghCommit.getSha())) { totalSkipped++; continue; }
                    existingShas.add(ghCommit.getSha());
                    try {
                        Commit commit = mapToEntity(ghCommit, null, connection.getProjectId(), connection.getId(), branch);
                        savedCommits.add(commitRepository.save(commit));
                        totalSynced++;
                    } catch (Exception e) {
                        log.warn("Failed to save commit {}: {}", ghCommit.getSha(), e.getMessage());
                        totalSkipped++;
                    }
                }
            }

            // Update lastSyncAt
            connection.setLastSyncAt(Instant.now());
            connectionRepository.save(connection);
            log.info("Synced repo {}/{}: {} commits (stats loading in background)", owner, repo, totalSynced);

            // Background: fetch stats (lines added/deleted/files changed) asynchronously
            String bgOwner = owner, bgRepo = repo;
            String bgToken = connection.getAccessTokenEncrypted();
            Integer bgConnectionId = connection.getId();

            java.util.concurrent.CompletableFuture.runAsync(() -> {
                // 1) Fetch stats for NEW commits
                if (!savedCommits.isEmpty()) {
                    log.info("Background stats fetch started for {} NEW commits", savedCommits.size());
                    List<java.util.concurrent.CompletableFuture<Void>> futures = savedCommits.stream()
                            .map(c -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                    GitHubApiClient.GitHubCommitDetailDto detail = gitHubApiClient
                                            .fetchCommitDetails(bgOwner, bgRepo, c.getCommitSha(), bgToken);
                                    if (detail != null && detail.getStats() != null) {
                                        c.setLinesAdded(detail.getStats().getAdditions() != null ? detail.getStats().getAdditions() : 0);
                                        c.setLinesDeleted(detail.getStats().getDeletions() != null ? detail.getStats().getDeletions() : 0);
                                        c.setFilesChanged(detail.getFiles() != null ? detail.getFiles().size() : 0);
                                        applyFilteredStats(c, detail.getFiles());
                                        commitRepository.save(c);
                                    }
                                } catch (Exception e) {
                                    log.debug("Stats fetch failed for {}: {}", c.getCommitSha(), e.getMessage());
                                }
                            }))
                            .collect(Collectors.toList());
                    try {
                        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                                .get(120, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception e) { log.debug("Some new stats fetches timed out"); }
                    log.info("Background stats fetch completed for {} NEW commits", savedCommits.size());
                }

                // 2) Backfill stats for EXISTING commits with missing stats (all zeros)
                List<Commit> missingStatsCommits = commitRepository.findByConnectionIdAndMissingStats(bgConnectionId);
                // Exclude merge commits (they legitimately have 0 stats)
                missingStatsCommits = missingStatsCommits.stream()
                        .filter(c -> {
                            String msg = c.getCommitMessage();
                            if (msg == null) return true;
                            String lower = msg.toLowerCase().trim();
                            return !lower.startsWith("merge pull request")
                                    && !lower.startsWith("merge branch")
                                    && !lower.startsWith("merge remote");
                        })
                        .collect(Collectors.toList());

                if (!missingStatsCommits.isEmpty()) {
                    log.info("Backfilling stats for {} existing commits with missing stats", missingStatsCommits.size());
                    int backfillSuccess = 0;
                    // Process in small batches to avoid rate limiting
                    int batchSize = 10;
                    for (int i = 0; i < missingStatsCommits.size(); i += batchSize) {
                        List<Commit> batch = missingStatsCommits.subList(i,
                                Math.min(i + batchSize, missingStatsCommits.size()));
                        List<java.util.concurrent.CompletableFuture<Void>> backfillFutures = batch.stream()
                                .map(c -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                                    try {
                                        GitHubApiClient.GitHubCommitDetailDto detail = gitHubApiClient
                                                .fetchCommitDetails(bgOwner, bgRepo, c.getCommitSha(), bgToken);
                                        if (detail != null && detail.getStats() != null
                                                && detail.getStats().getAdditions() != null) {
                                            c.setLinesAdded(detail.getStats().getAdditions());
                                            c.setLinesDeleted(detail.getStats().getDeletions() != null
                                                    ? detail.getStats().getDeletions() : 0);
                                            c.setFilesChanged(detail.getFiles() != null
                                                    ? detail.getFiles().size() : 0);
                                            applyFilteredStats(c, detail.getFiles());
                                            commitRepository.save(c);
                                        }
                                    } catch (Exception e) {
                                        log.debug("Backfill stats failed for {}: {}", c.getCommitSha(), e.getMessage());
                                    }
                                }))
                                .collect(Collectors.toList());
                        try {
                            java.util.concurrent.CompletableFuture.allOf(
                                    backfillFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
                                    .get(60, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (Exception e) { log.debug("Batch backfill timed out at index {}", i); }
                        // Throttle between batches
                        if (i + batchSize < missingStatsCommits.size()) {
                            try { Thread.sleep(500); } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    log.info("Backfill stats completed for {} commits", missingStatsCommits.size());
                }
            });
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

        // Build connectionId → repoLabel lookup 
        Map<Integer, String> connLabelMap = connectionRepository.findByProjectId(projectId)
                .stream()
                .collect(Collectors.toMap(
                        Connection::getId,
                        c -> c.getRepoLabel() != null ? c.getRepoLabel() : "",
                        (a, b) -> a));

        return commits.stream()
                .map(c -> {
                    CommitResponse resp = mapToResponse(c);
                    resp.setRepoLabel(connLabelMap.getOrDefault(c.getConnectionId(), ""));
                    return resp;
                })
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
     * Smart stats calculation using GitHub Statistics API for accurate lines added/deleted.
     * Commit counts are from DB (non-merge), but line stats come directly from GitHub's
     * Statistics API (/stats/contributors) which matches the Contributors page exactly.
     */
    private List<StatsResponse> calculateMergedStats(Integer projectId, Integer connectionId) {
        List<Commit> rawCommits = connectionId != null
                ? commitRepository.findByConnectionIdOrderByCommitDateDesc(connectionId)
                : commitRepository.findByProjectIdOrderByCommitDateDesc(projectId);
        if (rawCommits.isEmpty())
            return List.of();

        // Try to get accurate stats from GitHub Statistics API
        Connection connection = null;
        if (connectionId != null) {
            connection = connectionRepository.findById(connectionId).orElse(null);
        } else {
            // Find any active connection for this project
            List<Connection> conns = connectionRepository.findByProjectId(projectId);
            connection = conns.stream()
                    .filter(c -> c.getStatus() == com.trackspace.github.ConnectionStatus.CONNECTED)
                    .findFirst().orElse(null);
        }

        // Fetch GitHub Statistics API data
        java.util.Map<String, long[]> githubStats = new java.util.HashMap<>(); // login -> [additions, deletions, commits]
        if (connection != null) {
            try {
                String[] ownerRepo = parseRepositoryUrl(connection.getRepositoryUrl());
                String token = connection.getAccessTokenEncrypted();
                List<GitHubApiClient.ContributorStatsDto> contributorStats =
                        gitHubApiClient.fetchContributorStats(ownerRepo[0], ownerRepo[1], token);
                for (GitHubApiClient.ContributorStatsDto cs : contributorStats) {
                    if (cs.getAuthor() != null && cs.getAuthor().getLogin() != null) {
                        githubStats.put(cs.getAuthor().getLogin().toLowerCase(),
                                new long[]{ cs.getTotalAdditions(), cs.getTotalDeletions(), cs.getTotal() != null ? cs.getTotal() : 0 });
                    }
                }
                log.info("Fetched GitHub contributor stats for {} users", githubStats.size());
            } catch (Exception e) {
                log.warn("Failed to fetch GitHub contributor stats, falling back to DB stats: {}", e.getMessage());
            }
        }

        // Exclude merge commits for commit counting
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

        // Group by githubLogin — collect display names from ALL commits
        Map<String, Set<String>> loginToNames = new java.util.LinkedHashMap<>();
        for (Commit c : rawCommits) {
            String login = c.getGithubLogin() != null ? c.getGithubLogin().toLowerCase()
                    : (c.getAuthorEmail() != null ? c.getAuthorEmail().toLowerCase() : "unknown");
            String name = c.getAuthorName() != null ? c.getAuthorName().trim() : "Unknown";
            loginToNames.computeIfAbsent(login, k -> new java.util.HashSet<>()).add(name);
        }

        // Group non-merge commits for counting
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
            String login = entry.getKey();

            // Prefer full name (contains space) over username
            String displayName = names.stream()
                    .filter(n -> n.contains(" "))
                    .max(java.util.Comparator.comparingInt(String::length))
                    .orElse(names.stream()
                            .max(java.util.Comparator.comparingInt(String::length))
                            .orElse("Unknown"));

            long totalAdded;
            long totalDeleted;

            // Use GitHub Statistics API data if available, otherwise fall back to DB
            if (githubStats.containsKey(login)) {
                long[] gs = githubStats.get(login);
                totalAdded = gs[0];
                totalDeleted = gs[1];
            } else {
                totalAdded = groupCommits.stream().mapToLong(c -> c.getLinesAdded() != null ? c.getLinesAdded() : 0).sum();
                totalDeleted = groupCommits.stream().mapToLong(c -> c.getLinesDeleted() != null ? c.getLinesDeleted() : 0).sum();
            }

            Instant lastCommit = groupCommits.stream().map(Commit::getCommitDate).filter(d -> d != null)
                    .max(Instant::compareTo).orElse(null);

            result.add(StatsResponse.builder()
                    .userId(null)
                    .userName(displayName)
                    .githubLogin(login)
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
     * Fetch commits for a specific branch directly from GitHub API (real-time).
     * Enriches with DB stats if the commit SHA exists in our database.
     */
    @Override
    public List<CommitResponse> getCommitsByBranch(Integer projectId, Integer connectionId, String branch) {
        log.info("Fetching commits for branch '{}' from GitHub API (connection {})", branch, connectionId);

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + connectionId));

        // Parse owner/repo
        String[] ownerRepo = parseRepositoryUrl(connection.getRepositoryUrl());
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];

        // Fetch commits for this branch from GitHub API (last 365 days)
        Instant since = Instant.now().minus(Duration.ofDays(365));
        List<GitHubCommitDto> githubCommits = gitHubApiClient
                .fetchCommits(owner, repo, connection.getAccessTokenEncrypted(), since, branch);

        if (githubCommits == null || githubCommits.isEmpty()) {
            return Collections.emptyList();
        }

        // Pre-load all DB commits by SHA for fast enrichment (stats lookup)
        Map<String, Commit> dbCommitsBySha = commitRepository
                .findByConnectionIdOrderByCommitDateDesc(connectionId)
                .stream()
                .collect(Collectors.toMap(Commit::getCommitSha, c -> c, (a, b) -> a));

        // Map GitHub API commits to CommitResponse, enriching with DB stats
        List<CommitResponse> result = new ArrayList<>();
        for (GitHubCommitDto ghCommit : githubCommits) {
            Commit dbCommit = dbCommitsBySha.get(ghCommit.getSha());

            CommitResponse.CommitResponseBuilder builder = CommitResponse.builder()
                    .commitSha(ghCommit.getSha())
                    .commitMessage(ghCommit.getCommit().getMessage())
                    .authorName(ghCommit.getCommit().getAuthor().getName())
                    .authorEmail(ghCommit.getCommit().getAuthor().getEmail())
                    .commitDate(Instant.parse(ghCommit.getCommit().getAuthor().getDate()))
                    .branchName(branch);

            // GitHub login
            if (ghCommit.getAuthor() != null && ghCommit.getAuthor().getLogin() != null) {
                builder.githubLogin(ghCommit.getAuthor().getLogin());
            } else if (ghCommit.getCommitter() != null && ghCommit.getCommitter().getLogin() != null) {
                builder.githubLogin(ghCommit.getCommitter().getLogin());
            }

            // Enrich with DB stats if available
            if (dbCommit != null) {
                builder.commitId(dbCommit.getId())
                        .filesChanged(dbCommit.getFilesChanged())
                        .linesAdded(dbCommit.getLinesAdded())
                        .linesDeleted(dbCommit.getLinesDeleted())
                        .authorId(dbCommit.getAuthorId())
                        .linkedIssueId(dbCommit.getLinkedIssueId());
            } else {
                builder.commitId(null)
                        .filesChanged(0)
                        .linesAdded(0)
                        .linesDeleted(0);
            }

            result.add(builder.build());
        }

        log.info("Fetched {} commits for branch '{}' from GitHub API", result.size(), branch);
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
        // Fallback: if author is null (email not matching any GitHub account), try committer
        if (githubCommit.getAuthor() != null && githubCommit.getAuthor().getLogin() != null) {
            commit.setGithubLogin(githubCommit.getAuthor().getLogin());
        } else if (githubCommit.getCommitter() != null && githubCommit.getCommitter().getLogin() != null) {
            commit.setGithubLogin(githubCommit.getCommitter().getLogin());
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
            applyFilteredStats(commit, detail.getFiles());
        } else {
            commit.setLinesAdded(0);
            commit.setLinesDeleted(0);
            commit.setLinesAddedCode(0);
            commit.setLinesDeletedCode(0);
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

    // ─────────────────────────────────────────────────────────────────────────
    // Library file filter (for Contribution analytics)
    // ─────────────────────────────────────────────────────────────────────────

    /** Path prefixes that indicate library/generated/vendored files. */
    private static final java.util.Set<String> EXCLUDED_PREFIXES = java.util.Set.of(
            "node_modules/", ".next/", "dist/", "build/", "out/",
            "vendor/", "public/lib/", ".yarn/", ".pnp.",
            ".nuxt/", "coverage/", "__pycache__/", ".venv/", ".agent/"
    );

    /** Exact filenames that are auto-generated / lock files. */
    private static final java.util.Set<String> EXCLUDED_FILES = java.util.Set.of(
            "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "composer.lock", "gemfile.lock", "poetry.lock",
            ".ds_store", "thumbs.db"
    );

    /**
     * Returns true if the file path belongs to a library, generated, or vendored file
     * that should NOT count toward contribution metrics.
     */
    static boolean isLibraryFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase().replace('\\', '/');

        // Check basename against excluded filenames
        String basename = lower.contains("/") ? lower.substring(lower.lastIndexOf('/') + 1) : lower;
        if (EXCLUDED_FILES.contains(basename)) return true;

        // Check path prefixes
        for (String prefix : EXCLUDED_PREFIXES) {
            if (lower.startsWith(prefix) || lower.contains("/" + prefix)) return true;
        }

        // Minified bundles
        return lower.endsWith(".min.js") || lower.endsWith(".min.css") || lower.endsWith(".map");
    }

    /**
     * Calculates lines added/deleted excluding library files and sets
     * the filtered columns on the commit entity.
     */
    private void applyFilteredStats(Commit commit,
                                    java.util.List<GitHubApiClient.GitHubCommitDetailDto.FileChange> files) {
        if (files == null || files.isEmpty()) {
            // No per-file data — fall back to total stats
            commit.setLinesAddedCode(commit.getLinesAdded());
            commit.setLinesDeletedCode(commit.getLinesDeleted());
            // Weighted = raw added * default weight (no per-file info available)
            int raw = commit.getLinesAdded() != null ? commit.getLinesAdded() : 0;
            commit.setWeightedLinesAdded(raw * MetricsCalculator.DEFAULT_FILE_WEIGHT);
            return;
        }

        int addedCode = 0;
        int deletedCode = 0;
        double weightedAdded = 0.0;
        for (GitHubApiClient.GitHubCommitDetailDto.FileChange f : files) {
            if (!isLibraryFile(f.getFilename())) {
                int additions = f.getAdditions() != null ? f.getAdditions() : 0;
                int deletions = f.getDeletions() != null ? f.getDeletions() : 0;
                addedCode   += additions;
                deletedCode += deletions;
                weightedAdded += additions * MetricsCalculator.getFileWeight(f.getFilename());
            }
        }
        commit.setLinesAddedCode(addedCode);
        commit.setLinesDeletedCode(deletedCode);
        commit.setWeightedLinesAdded(weightedAdded);
    }
}
