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
    // ── V2 Scoring Pillars ──
    /** Code Score (0–100): Normalized weighted lines added */
    private double codeScore;

    /** Task Score (0–100): Tasks completed / Assigned */
    private double taskScore;

    /** Consistency Score (0–100): Active days ratio */
    private double consistencyScore;

    // ── Extra Metric Fields ──
    private int activeDays;
    private double weightedLinesAdded;
    private int overdueTaskCount;
    private String role;

    // ── Task Stats ──
    private int tasksAssigned;
    private int tasksCompleted;
    private int tasksInProgress;
    /** taskCompletionRate as percentage 0–100 */
    private double taskCompletionRate;

    // ── Final Score ──
    /** Overall score 0–100: 0.5 * githubImpactScore + 0.5 * jiraExecutionScore */
    private double contributionScore;

    // ── Issue Detection Flags ──
    private boolean inactive;
    private boolean hasLowContribution;

    private Instant lastActivityDate;
    private Instant calculatedAt;
}
