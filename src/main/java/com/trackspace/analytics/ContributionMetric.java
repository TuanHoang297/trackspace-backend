package com.trackspace.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ContributionMetric Entity
 *
 * Stores the calculated contribution scores for each user in a project.
 * Updated/recalculated on demand or on scheduled sync.
 */
@Entity
@Table(
        name = "contribution_metrics",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"project_id", "user_id"},
                name = "unique_project_user"
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long id;

    // project_id matches github/jira convention (Integer)
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    // user_id matches User entity (Long → INT column via JPA)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ── Jira Task Metrics ──
    @Builder.Default
    @Column(name = "tasks_assigned")
    private Integer tasksAssigned = 0;

    @Builder.Default
    @Column(name = "tasks_completed")
    private Integer tasksCompleted = 0;

    @Builder.Default
    @Column(name = "tasks_in_progress")
    private Integer tasksInProgress = 0;

    @Builder.Default
    @Column(name = "task_completion_rate")
    private Double taskCompletionRate = 0.0;

    // ── GitHub Code Metrics ──
    @Builder.Default
    @Column(name = "total_commits")
    private Integer totalCommits = 0;

    @Builder.Default
    @Column(name = "lines_added")
    private Integer linesAdded = 0;

    @Builder.Default
    @Column(name = "lines_deleted")
    private Integer linesDeleted = 0;

    @Builder.Default
    @Column(name = "weighted_lines_added")
    private Double weightedLinesAdded = 0.0;

    // ── Extra Metric Fields ──
    @Builder.Default
    @Column(name = "active_days")
    private Integer activeDays = 0;

    @Builder.Default
    @Column(name = "overdue_task_count")
    private Integer overdueTaskCount = 0;

    @Column(name = "role")
    private String role;

    // ── V2 Scoring Pillars ──

    /** Code Score (0–100): Normalized weighted lines added */
    @Builder.Default
    @Column(name = "code_score")
    private Double codeScore = 0.0;

    /** Task Score (0–100): Tasks completed / Assigned */
    @Builder.Default
    @Column(name = "task_score")
    private Double taskScore = 0.0;

    /** Consistency Score (0–100): Active days ratio */
    @Builder.Default
    @Column(name = "consistency_score")
    private Double consistencyScore = 0.0;

    // ── Final Score ──

    /** Overall contribution score (0–100): 50% GitHub + 50% Jira */
    @Builder.Default
    @Column(name = "contribution_score")
    private Double contributionScore = 0.0;

    // ── Issue Detection Flags ──

    @Builder.Default
    @Column(name = "is_inactive")
    private Boolean inactive = false;

    @Builder.Default
    @Column(name = "has_low_contribution")
    private Boolean hasLowContribution = false;

    @Column(name = "last_activity_date")
    private Instant lastActivityDate;

    @Builder.Default
    @Column(name = "calculated_at")
    private Instant calculatedAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        calculatedAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
