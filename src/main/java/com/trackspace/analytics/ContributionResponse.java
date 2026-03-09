package com.trackspace.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Per-user contribution breakdown for a project.
 *
 * Contains all four pillars of the Impact &amp; Consistency Score:
 *  1. GitHub Impact (Log10 + file weight + bug multiplier)
 *  2. Consistency (active days factor)
 *  3. Jira Execution (completion rate + quality penalty)
 *  4. Final combined score (50/50)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionResponse {

    // ── Identity ──
    private Long userId;
    private String fullName;
    private String email;

    // ── GitHub Pillar (50 %) ──
    private int totalCommits;
    private int linesAdded;
    private int linesDeleted;
    /** Commits identified as bug-fixes by message scanning */
    private int bugFixCommits;
    /** Normalized GitHub Impact Score 0–100 */
    private double githubImpactScore;

    // ── Consistency Pillar ──
    /** Distinct calendar days with at least one commit */
    private int activeDays;
    /** Multiplier applied on top of GitHub raw score */
    private double consistencyFactor;

    // ── Jira Pillar (50 %) ──
    private int tasksAssigned;
    private int tasksCompleted;
    private int tasksInProgress;
    /** taskCompletionRate as percentage 0–100 */
    private double taskCompletionRate;
    /** Bug tasks linked to this user's original tasks (quality penalty signal) */
    private int reworkCount;
    /** Normalized Jira Execution Score 0–100 */
    private double jiraExecutionScore;

    // ── Code Churn ──
    /**
     * linesDeleted / (linesAdded + 1).
     * High value means the user wrote code that was later heavily reworked.
     */
    private double codeChurnRate;

    // ── Domain ──
    /**
     * Primary domain: "FRONTEND", "BACKEND", "BOTH", or "UNKNOWN".
     * GitHub score is normalised only against members of the same domain,
     * ensuring FE students compete with FE and BE students with BE.
     */
    private String domain;

    /**
     * Smart Coder Bonus [1.0, 1.5] applied to Jira Execution Score.
     * Rewards efficient coders who close many tasks with compact code.
     */
    private double smartCoderBonus;

    // ── Final Score ──
    /** Overall score 0–100: 0.5 * githubImpactScore + 0.5 * jiraExecutionScore */
    private double contributionScore;

    // ── Issue Detection Flags ──
    private boolean inactive;
    private boolean hasLowContribution;
    private boolean hasOverdueTasks;

    private Instant lastActivityDate;
    private Instant calculatedAt;
}
