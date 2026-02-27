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
     * Get commits for a project
     * 
     * @param projectId Project ID
     * @param userId    Optional user ID filter
     * @param since     Optional start date filter
     * @param until     Optional end date filter
     * @return List of commit responses
     */
    List<CommitResponse> getCommits(Integer projectId, Integer userId, Instant since, Instant until);

    /**
     * Get contribution statistics for a project
     * 
     * @param projectId Project ID
     * @param userId    Optional user ID filter (null = all users)
     * @return List of stats responses
     */
    List<StatsResponse> getStats(Integer projectId, Integer userId);
}
