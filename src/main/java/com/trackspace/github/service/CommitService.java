package com.trackspace.github.service;

import com.trackspace.github.dto.CommitResponse;
import com.trackspace.github.dto.StatsResponse;
import com.trackspace.github.dto.SyncRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing GitHub commits
 */
public interface CommitService {

    /**
     * Sync commits from GitHub repositor
     */
    Map<String, Object> syncCommits(SyncRequest request);

    /**
     * Sync a single connection (triggered by webhook push event)
     *
     * @param connectionId ID of the specific GitHub connection to sync
     * @return sync result map
     */
    Map<String, Object> syncSingleConnection(Integer connectionId);

    /**
     * Get commits for a project (optionally scoped to a single connection/repo)
     * 
     * @param projectId    Project ID
     * @param connectionId Optional connection ID to filter by specific repo
     * @param userId       Optional user ID filter
     * @param since        Optional start date filter
     * @param until        Optional end date filter
     * @param branch       Optional branch name filter
     * @return List of commit responses
     */
    List<CommitResponse> getCommits(Integer projectId, Integer connectionId, Integer userId, Instant since,
            Instant until, String branch);

    /**
     * Get contribution statistics for a project (optionally scoped to a single
     * connection/repo)
     * 
     * @param projectId    Project ID
     * @param connectionId Optional connection ID (null = all repos)
     * @param userId       Optional user ID filter (null = all users)
     * @return List of stats responses
     */
    List<StatsResponse> getStats(Integer projectId, Integer connectionId, Integer userId);

    /**
     * Get commits for a specific branch directly from GitHub API (real-time).
     * Used when filtering by non-default branches since DB only stores
     * default branch commits.
     *
     * @param projectId    Project ID
     * @param connectionId Connection ID (required — scopes to a specific repo)
     * @param branch       Branch name to fetch commits from
     * @return List of commit responses from GitHub API enriched with DB stats
     */
    List<CommitResponse> getCommitsByBranch(Integer projectId, Integer connectionId, String branch);
}
