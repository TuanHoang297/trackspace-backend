package com.trackspace.analytics;

import com.trackspace.github.entity.Commit;
import com.trackspace.github.entity.Connection;
import com.trackspace.jira.entity.JiraIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V2 Contribution Calculator
 * Formula: Final = 40% Code + 40% Task + 20% Consistency
 *
 * - Code:        per-file weighted lines (sum of weightedLinesAdded per commit, skip merge)
 * - Task:        tasksCompleted / tasksAssigned
 * - Consistency: min(activeDays / (projectWeeks × 3), 1.0)
 */
@Component
@Slf4j
public class ContributionCalculator {

    private static final double CODE_WEIGHT = 0.4;
    private static final double TASK_WEIGHT = 0.4;
    private static final double CONSISTENCY_WEIGHT = 0.2;
    private static final int EXPECTED_ACTIVE_DAYS_PER_WEEK = 3;

    /**
     * Main calculation entry point.
     * Returns a list of ContributionMetric entities ready for persistence.
     */
    public List<ContributionMetric> calculate(
            Integer projectId,
            List<Integer> memberUserIds,
            List<Commit> allCommits,
            List<JiraIssue> allIssues,
            List<Connection> connections) {

        if (memberUserIds == null || memberUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Build connection → repoLabel map for role detection
        Map<Integer, String> connectionLabelMap = buildConnectionLabelMap(connections);

        // Group commits & issues by authorId / assigneeId
        Map<Integer, List<Commit>> commitsByUser = allCommits.stream()
                .filter(c -> c.getAuthorId() != null)
                .collect(Collectors.groupingBy(Commit::getAuthorId));

        Map<Integer, List<JiraIssue>> issuesByUser = allIssues.stream()
                .filter(i -> i.getAssigneeId() != null)
                .collect(Collectors.groupingBy(JiraIssue::getAssigneeId));

        // Compute project-wide date range for consistency calculation
        long projectWeeks = computeProjectWeeks(allCommits);

        // Compute intermediate results per member
        List<MemberResult> results = new ArrayList<>();
        for (Integer userId : memberUserIds) {
            List<Commit> userCommits = commitsByUser.getOrDefault(userId, Collections.emptyList());
            List<JiraIssue> userIssues = issuesByUser.getOrDefault(userId, Collections.emptyList());

            MemberResult r = computeMember(userId, userCommits, userIssues, projectWeeks, connectionLabelMap);
            results.add(r);
        }

        // Normalize code scores (0-100) relative to group max
        double maxCodeRaw = results.stream().mapToDouble(r -> r.codeRaw).max().orElse(1.0);
        if (maxCodeRaw <= 0) maxCodeRaw = 1.0;

        for (MemberResult r : results) {
            r.codeScore = (r.codeRaw / maxCodeRaw) * 100.0;
        }

        // Normalize task scores (0-100) relative to group max completed tasks
        int maxCompleted = results.stream().mapToInt(r -> r.tasksCompleted).max().orElse(1);
        if (maxCompleted <= 0) maxCompleted = 1;

        for (MemberResult r : results) {
            double baseTaskScore = ((double) r.tasksCompleted / maxCompleted) * 100.0;
            double penalty = 0.0;
            if (r.tasksAssigned > 0) {
                double valuePerTask = 100.0 / r.tasksAssigned;
                penalty = r.overdueCount * (valuePerTask * 0.5);
            }
            r.taskScore = Math.max(0.0, baseTaskScore - penalty);
        }

        // Compute final scores and redistribute
        double totalFinal = 0.0;
        for (MemberResult r : results) {
            r.finalScore = CODE_WEIGHT * r.codeScore
                         + TASK_WEIGHT * r.taskScore
                         + CONSISTENCY_WEIGHT * r.consistencyScore;
            totalFinal += r.finalScore;
        }

        // Build entities
        Instant now = Instant.now();
        List<ContributionMetric> metrics = new ArrayList<>();
        for (MemberResult r : results) {
            double contributionShare = totalFinal > 0 ? (r.finalScore / totalFinal) * 100.0 : 0.0;

            ContributionMetric m = ContributionMetric.builder()
                    .projectId(projectId)
                    .userId((long) r.userId)
                    .totalCommits(r.totalCommits)
                    .linesAdded(r.totalLinesAdded)
                    .linesDeleted(r.totalLinesDeleted)
                    .codeScore(r.codeScore)
                    .taskScore(r.taskScore)
                    .consistencyScore(r.consistencyScore)
                    .contributionScore(contributionShare)     // redistributed to sum=100%
                    .activeDays(r.activeDays)
                    .weightedLinesAdded(r.codeRaw)
                    .overdueTaskCount(r.overdueCount)
                    .role(r.role)
                    .tasksAssigned(r.tasksAssigned)
                    .tasksCompleted(r.tasksCompleted)
                    .tasksInProgress(r.tasksInProgress)
                    .taskCompletionRate(r.taskCompletionRate)
                    .lastActivityDate(r.lastActivityDate)
                    .inactive(r.isInactive)
                    .hasLowContribution(false) // set after all scores computed
                    .calculatedAt(now)
                    .updatedAt(now)
                    .build();

            metrics.add(m);
        }

        // Flag low contribution (< 20% of average share)
        double avgShare = memberUserIds.isEmpty() ? 0 : 100.0 / memberUserIds.size();
        double lowThreshold = avgShare * 0.2;
        for (ContributionMetric m : metrics) {
            m.setHasLowContribution(m.getContributionScore() < lowThreshold);
        }

        log.info("V2 Contribution calculated for project {} — {} members", projectId, metrics.size());
        return metrics;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-member computation
    // ─────────────────────────────────────────────────────────────────────────

    private MemberResult computeMember(
            Integer userId,
            List<Commit> commits,
            List<JiraIssue> issues,
            long projectWeeks,
            Map<Integer, String> connectionLabelMap) {

        MemberResult r = new MemberResult();
        r.userId = userId;

        // ── Code metrics ──
        Set<LocalDate> activeDates = new HashSet<>();
        int totalAdded = 0;
        int totalDeleted = 0;
        double weightedSum = 0.0;
        int bugFixes = 0;
        Instant lastCommitDate = null;

        for (Commit c : commits) {
            r.totalCommits++;

            // Skip merge commits
            if (MetricsCalculator.isMergeCommit(c.getCommitMessage())) {
                continue;
            }

            // Weighted lines (pre-computed during sync)
            double w = c.getWeightedLinesAdded() != null ? c.getWeightedLinesAdded() : 0.0;
            weightedSum += w;

            // Raw lines for display
            int added = c.getLinesAddedCode() != null ? c.getLinesAddedCode()
                       : (c.getLinesAdded() != null ? c.getLinesAdded() : 0);
            int deleted = c.getLinesDeletedCode() != null ? c.getLinesDeletedCode()
                         : (c.getLinesDeleted() != null ? c.getLinesDeleted() : 0);
            totalAdded += added;
            totalDeleted += deleted;

            // Active days
            if (c.getCommitDate() != null) {
                activeDates.add(c.getCommitDate().atZone(ZoneId.systemDefault()).toLocalDate());
                if (lastCommitDate == null || c.getCommitDate().isAfter(lastCommitDate)) {
                    lastCommitDate = c.getCommitDate();
                }
            }

            // Bug fix detection (for display only, not used in scoring)
            if (MetricsCalculator.isBugFix(c.getCommitMessage())) {
                bugFixes++;
            }

            // Track which domains this user commits to
            if (c.getConnectionId() != null) {
                String label = connectionLabelMap.getOrDefault(c.getConnectionId(), "");
                if ("FRONTEND".equalsIgnoreCase(label)) r.hasFE = true;
                else if ("BACKEND".equalsIgnoreCase(label)) r.hasBE = true;
            }
        }

        r.codeRaw = weightedSum;
        r.totalLinesAdded = totalAdded;
        r.totalLinesDeleted = totalDeleted;
        r.activeDays = activeDates.size();
        r.bugFixCommits = bugFixes;
        r.codeChurnRate = totalAdded > 0 ? (double) totalDeleted / totalAdded : 0.0;

        // Last activity
        r.lastActivityDate = lastCommitDate;
        r.isInactive = lastCommitDate != null
                && ChronoUnit.DAYS.between(lastCommitDate, Instant.now()) >= 3;

        // ── Role detection (display only) ──
        if (r.hasFE && r.hasBE)      r.role = "FULLSTACK";
        else if (r.hasFE)            r.role = "FRONTEND";
        else if (r.hasBE)            r.role = "BACKEND";
        else                         r.role = "UNKNOWN";

        // ── Task metrics ──
        int assigned = issues.size();
        int completed = 0;
        int inProgress = 0;
        int overdue = 0;

        for (JiraIssue issue : issues) {
            String status = issue.getStatus() != null ? issue.getStatus().toUpperCase() : "";
            if ("DONE".equals(status) || "CLOSED".equals(status) || "RESOLVED".equals(status)) {
                completed++;
            } else if ("IN PROGRESS".equals(status) || "IN_PROGRESS".equals(status)) {
                inProgress++;
            }
            // Check overdue
            if (issue.getDueDate() != null
                    && LocalDate.now().isAfter(issue.getDueDate())
                    && !"DONE".equalsIgnoreCase(issue.getStatus())
                    && !"CLOSED".equalsIgnoreCase(issue.getStatus())
                    && !"RESOLVED".equalsIgnoreCase(issue.getStatus())) {
                overdue++;
            }
        }

        r.tasksAssigned = assigned;
        r.tasksCompleted = completed;
        r.tasksInProgress = inProgress;
        r.overdueCount = overdue;
        r.taskCompletionRate = assigned > 0 ? ((double) completed / assigned) * 100.0 : 0.0;
        // taskScore will be normalized after all members are computed (like codeScore)

        // ── Consistency score ──
        if (projectWeeks > 0) {
            double expectedDays = projectWeeks * EXPECTED_ACTIVE_DAYS_PER_WEEK;
            r.consistencyScore = Math.min(r.activeDays / expectedDays, 1.0) * 100.0;
        } else {
            r.consistencyScore = r.activeDays > 0 ? 100.0 : 0.0;
        }

        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<Integer, String> buildConnectionLabelMap(List<Connection> connections) {
        if (connections == null) return Collections.emptyMap();
        Map<Integer, String> map = new HashMap<>();
        for (Connection c : connections) {
            if (c.getId() != null && c.getRepoLabel() != null) {
                map.put(c.getId(), c.getRepoLabel());
            }
        }
        return map;
    }

    /**
     * Computes project duration in weeks based on the earliest and latest commit dates.
     * Minimum 1 week to avoid division by zero.
     */
    private long computeProjectWeeks(List<Commit> allCommits) {
        Instant earliest = null;
        Instant latest = null;
        for (Commit c : allCommits) {
            if (c.getCommitDate() == null) continue;
            if (earliest == null || c.getCommitDate().isBefore(earliest)) earliest = c.getCommitDate();
            if (latest == null || c.getCommitDate().isAfter(latest)) latest = c.getCommitDate();
        }
        if (earliest == null || latest == null) return 1;
        long days = ChronoUnit.DAYS.between(earliest, latest);
        long weeks = Math.max(1, days / 7);
        return weeks;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal DTO
    // ─────────────────────────────────────────────────────────────────────────

    private static class MemberResult {
        int userId;

        // Code
        double codeRaw;       // Σ weightedLinesAdded (before normalize)
        double codeScore;     // normalized 0-100
        int totalCommits;
        int totalLinesAdded;
        int totalLinesDeleted;
        int activeDays;
        int bugFixCommits;
        double codeChurnRate;

        // Task
        int tasksAssigned;
        int tasksCompleted;
        int tasksInProgress;
        int overdueCount;
        double taskCompletionRate;
        double taskScore;     // 0-100

        // Consistency
        double consistencyScore; // 0-100

        // Role
        boolean hasFE;
        boolean hasBE;
        String role = "UNKNOWN";

        // Activity
        Instant lastActivityDate;
        boolean isInactive;

        // Final
        double finalScore;
    }
}
