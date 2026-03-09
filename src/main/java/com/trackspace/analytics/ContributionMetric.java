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

    // ── Advanced Scoring (added via ddl-auto=update) ──

    /** GitHub Impact Score (0–100): Log10 + bug multiplier, normalized */
    @Builder.Default
    @Column(name = "github_impact_score")
    private Double githubImpactScore = 0.0;

    /** Jira Execution Score (0–100): completion rate * quality factor */
    @Builder.Default
    @Column(name = "jira_execution_score")
    private Double jiraExecutionScore = 0.0;

    /** Consistency multiplier applied during calculation */
    @Builder.Default
    @Column(name = "consistency_factor")
    private Double consistencyFactor = 1.0;

    /** Days on which the user made at least one commit */
    @Builder.Default
    @Column(name = "active_days")
    private Integer activeDays = 0;

    /** Commits whose message indicates a bug-fix */
    @Builder.Default
    @Column(name = "bug_fix_commits")
    private Integer bugFixCommits = 0;

    /**
     * Code churn rate: linesDeleted / (linesAdded + 1).
     * High values flag potential "throw-away" or copied code.
     */
    @Builder.Default
    @Column(name = "code_churn_rate")
    private Double codeChurnRate = 0.0;

    /**
     * Number of Jira tasks reworked by the user (i.e. Bug issues
     * linked to tasks originally assigned to them).
     */
    @Builder.Default
    @Column(name = "rework_count")
    private Integer reworkCount = 0;

    // ── Domain & Smart Coder ──

    /**
     * Primary domain derived from which GitHub repo the user commits most to.
     * Values: "FRONTEND", "BACKEND", "BOTH", "UNKNOWN".
     * GitHub score is normalised within this domain group.
     */
    @Builder.Default
    @Column(name = "domain", length = 10)
    private String domain = "UNKNOWN";

    /**
     * Smart Coder Bonus multiplier applied to Jira Execution Score.
     * Rewards members who close many tasks with compact, efficient code.
     * Range [1.0, 1.5].
     */
    @Builder.Default
    @Column(name = "smart_coder_bonus")
    private Double smartCoderBonus = 1.0;

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

    @Builder.Default
    @Column(name = "has_overdue_tasks")
    private Boolean hasOverdueTasks = false;

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
