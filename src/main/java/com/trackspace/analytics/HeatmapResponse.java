package com.trackspace.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Activity heatmap data for a single user in a project.
 *
 * The entries list contains one record per day on which the user committed.
 * The frontend renders this as a GitHub-style contribution calendar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse {

    private Long userId;
    private String fullName;
    private Integer projectId;

    private List<HeatmapEntry> entries;

    private int totalActiveDays;
    private int totalCommits;
    private int totalLinesAdded;

    // ──────────────────────────────────────────────────────────────────────────
    // Nested DTO
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Represents commit activity on a single calendar day.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class HeatmapEntry {
        /** ISO date string, e.g. "2025-03-01" */
        private String date;
        private int commitCount;
        private int linesAdded;
        private int linesDeleted;
    }
}
