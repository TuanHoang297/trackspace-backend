package com.trackspace.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Detected anomalies / issues within a project's member contributions.
 *
 * Used by the lecturer dashboard to quickly identify at-risk students.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDetectionResponse {

    private Integer projectId;

    private List<MemberIssue> issues;

    // ──────────────────────────────────────────────────────────────────────────
    // Nested DTO
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * A single detected problem for one project member.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MemberIssue {
        private Long userId;
        private String userName;

        /**
         * Issue type:
         * INACTIVE            – no commit in ≥ 3 days
         * LOW_CONTRIBUTION    – score &lt; 20% of project average
         * OVERDUE_TASKS       – has Jira tasks past due-date
         * HIGH_CHURN          – code churn rate &gt; 1.5 (more deleted than added)
         * HIGH_REWORK         – caused multiple bug tickets from their tasks
         */
        private String issueType;

        private String description;
        private double currentScore;
    }
}
