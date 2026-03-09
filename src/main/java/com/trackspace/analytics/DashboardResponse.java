package com.trackspace.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Project-level analytics dashboard response.
 *
 * Provides aggregated metrics and chart-ready data for the lecturer view:
 *  - Radar / Bar chart: tasks completed vs. GitHub impact per member
 *  - Anomaly detection summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Integer projectId;

    // ── Aggregate Totals ──
    private int totalMembers;
    private int totalCommits;
    private int totalLinesAdded;
    private int totalTasksAssigned;
    private int totalTasksCompleted;
    /** Overall task completion rate across the whole project (%) */
    private double overallCompletionRate;

    /**
     * Distribution of Jira issue statuses across the project.
     * e.g. {"To Do": 4, "In Progress": 3, "Done": 8}
     */
    private Map<String, Long> issueStatusDistribution;

    // ── Per-member Data (sorted by contributionScore desc) ──
    private List<ContributionResponse> memberContributions;

    // ── Anomaly Summary ──
    /** Human-readable anomaly messages, e.g. "Nguyen Van A: inactive > 3 days" */
    private List<String> detectedAnomalies;
}
