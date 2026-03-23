package com.trackspace.analytics;

import com.trackspace.github.entity.Commit;
import com.trackspace.github.entity.Connection;
import com.trackspace.github.repository.CommitRepository;
import com.trackspace.github.repository.ConnectionRepository;
import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core analytics computation component.
 *
 * Implements the 4-pillar Impact &amp; Consistency Score:
 * <ol>
 *   <li><b>Domain Isolation</b> – commits tagged FRONTEND / BACKEND via
 *       {@code Connection.repoLabel}; GitHub scores normalised within each domain.</li>
 *   <li><b>Anti-Cheat &amp; Quality</b> – Log₁₀ compression, file-type weights,
 *       bug-fix multiplier.</li>
 *   <li><b>Cross-Validation</b> – 50% GitHub Impact + 50% Jira Execution.
 *       Smart Coder Bonus rewards task efficiency (tasks / log10 lines).</li>
 *   <li><b>Final Score 0–100</b> – combined via domain-weighted normalization.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContributionCalculator {

    // Domain label constants — match Connection.repoLabel (upper-cased)
    public static final String DOMAIN_FRONTEND = "FRONTEND";
    public static final String DOMAIN_BACKEND  = "BACKEND";
    public static final String DOMAIN_BOTH     = "BOTH";
    public static final String DOMAIN_UNKNOWN  = "UNKNOWN";

    private final CommitRepository commitRepository;
    private final JiraIssueRepository jiraIssueRepository;
    private final ConnectionRepository connectionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Calculate with default 50 / 50 domain weighting. */
    public List<ContributionMetric> calculate(Integer projectId, List<User> members) {
        return calculate(projectId, members, 0.5, 0.5);
    }

    /**
     * Calculate with explicit domain weights.
     *
     * @param feWeight weight applied to the FRONTEND normalised score (0–1)
     * @param beWeight weight applied to the BACKEND normalised score  (0–1)
     */
    public List<ContributionMetric> calculate(
            Integer projectId, List<User> members,
            double feWeight, double beWeight) {

        List<Commit> allCommits = commitRepository.findByProjectId(projectId);
        List<JiraIssue> allIssues  = jiraIssueRepository.findByProjectId(projectId);

        // ── Build connectionId → domain map ────────────────────────────────
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        Map<Integer, String> connDomain = connections.stream()
                .collect(Collectors.toMap(
                        Connection::getId,
                        c -> c.getRepoLabel() != null
                                ? c.getRepoLabel().toUpperCase().trim()
                                : DOMAIN_UNKNOWN,
                        (a, b) -> a));

        boolean hasDomainSplit = connDomain.values().stream()
                .anyMatch(d -> DOMAIN_FRONTEND.equals(d) || DOMAIN_BACKEND.equals(d));

        // ── Group raw data ─────────────────────────────────────────────────
        Map<Integer, List<Commit>>    commitsByAuthor  = allCommits.stream()
                .filter(c -> c.getAuthorId() != null)
                .collect(Collectors.groupingBy(Commit::getAuthorId));

        Map<Integer, List<JiraIssue>> issuesByAssignee = allIssues.stream()
                .filter(i -> i.getAssigneeId() != null)
                .collect(Collectors.groupingBy(JiraIssue::getAssigneeId));

        // ── Per-member intermediate results ────────────────────────────────
        List<IntermediateResult> intermediates = new ArrayList<>();
        for (User member : members) {
            intermediates.add(computeIntermediate(
                    projectId, member, allCommits,
                    commitsByAuthor, issuesByAssignee,
                    connDomain, hasDomainSplit));
        }

        // ── Normalise GitHub scores (domain-isolated or global) ────────────
        if (hasDomainSplit) {
            applyDomainNormalization(intermediates, feWeight, beWeight);
        } else {
            double maxRaw = intermediates.stream()
                    .mapToDouble(ir -> ir.rawGithubScore).max().orElse(1.0);
            intermediates.forEach(ir -> {
                ir.githubImpactScore = MetricsCalculator.normalizeScore(ir.rawGithubScore, maxRaw);
                ir.primaryDomain     = DOMAIN_UNKNOWN;
            });
        }

        // ── Jira Execution Score (absolute, not relative) ──────────────────
        // rawJiraScore = (completed/assigned) * jiraQuality * smartCoderBonus
        // All factors are in [0,1] range (smartCoderBonus max 1.5), so multiply
        // by 100 and cap at 100 to get an honest 0–100 score per member.
        intermediates.forEach(ir ->
                ir.jiraExecutionScore = Math.min(100.0, ir.rawJiraScore * 100.0));

        List<ContributionMetric> metrics = intermediates.stream().map(this::buildMetric).collect(Collectors.toList());

        // ── Redistribute contributionScore as proportional share (all members sum to 100%) ──
        double totalScore = metrics.stream().mapToDouble(ContributionMetric::getContributionScore).sum();
        if (totalScore > 0) {
            metrics.forEach(m -> m.setContributionScore(round2(m.getContributionScore() / totalScore * 100.0)));
        }

        return metrics;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Domain-isolated normalization
    // ─────────────────────────────────────────────────────────────────────────

    private void applyDomainNormalization(
            List<IntermediateResult> all, double feWeight, double beWeight) {

        double totalW = feWeight + beWeight;
        double normFE = totalW > 0 ? feWeight / totalW : 0.5;
        double normBE = totalW > 0 ? beWeight / totalW : 0.5;

        // Global max — fallback for UNKNOWN domain only
        double maxAll = all.stream().mapToDouble(ir -> ir.rawGithubScore).max().orElse(1.0);

        double maxFE = all.stream().mapToDouble(ir -> ir.feRawScore).max().orElse(1.0);
        double maxBE = all.stream().mapToDouble(ir -> ir.beRawScore).max().orElse(1.0);

        for (IntermediateResult ir : all) {
            boolean hasFE = ir.feRawScore > 0;
            boolean hasBE = ir.beRawScore > 0;

            double feScore = MetricsCalculator.normalizeScore(ir.feRawScore, maxFE);
            double beScore = MetricsCalculator.normalizeScore(ir.beRawScore, maxBE);

            if (hasFE && hasBE) {
                ir.githubImpactScore = feScore * normFE + beScore * normBE;
                ir.primaryDomain     = DOMAIN_BOTH;
            } else if (hasFE) {
                ir.githubImpactScore = feScore;
                ir.primaryDomain     = DOMAIN_FRONTEND;
            } else if (hasBE) {
                ir.githubImpactScore = beScore;
                ir.primaryDomain     = DOMAIN_BACKEND;
            } else {
                ir.githubImpactScore = MetricsCalculator.normalizeScore(ir.rawGithubScore, maxAll);
                ir.primaryDomain     = DOMAIN_UNKNOWN;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-member computation
    // ─────────────────────────────────────────────────────────────────────────

    private IntermediateResult computeIntermediate(
            Integer projectId,
            User member,
            List<Commit> allCommits,
            Map<Integer, List<Commit>>    commitsByAuthor,
            Map<Integer, List<JiraIssue>> issuesByAssignee,
            Map<Integer, String>          connDomain,
            boolean hasDomainSplit) {

        IntermediateResult ir = new IntermediateResult();
        ir.projectId = projectId;
        ir.user      = member;

        Integer authorId = member.getId().intValue();

        // ── GitHub (Pillar 1+2: log10 anti-cheat + quality multipliers) ────

        List<Commit> myCommits = commitsByAuthor.getOrDefault(authorId, List.of());
        ir.totalCommits = myCommits.size();

        int    totalAdded   = 0;
        int    totalDeleted = 0;
        int    bugFixes     = 0;
        double rawGit       = 0.0;
        double feRaw        = 0.0;
        double beRaw        = 0.0;
        Set<LocalDate> activeDays  = new HashSet<>();
        Instant lastActivity = null;

        for (Commit c : myCommits) {
            int added   = c.getLinesAdded()   != null ? c.getLinesAdded()   : 0;
            int deleted = c.getLinesDeleted() != null ? c.getLinesDeleted() : 0;
            totalAdded   += added;
            totalDeleted += deleted;

            double log10  = MetricsCalculator.calcLog10Score(added);
            double bugMul = MetricsCalculator.getBugMultiplier(c.getCommitMessage());
            double score  = log10 * bugMul * MetricsCalculator.DEFAULT_FILE_WEIGHT;

            rawGit += score;

            // Pillar 1: route to domain bucket
            if (hasDomainSplit && c.getConnectionId() != null) {
                String dom = connDomain.getOrDefault(c.getConnectionId(), DOMAIN_UNKNOWN);
                if (DOMAIN_FRONTEND.equals(dom))     feRaw += score;
                else if (DOMAIN_BACKEND.equals(dom)) beRaw += score;
            }

            if (MetricsCalculator.isBugFix(c.getCommitMessage())) bugFixes++;

            if (c.getCommitDate() != null) {
                activeDays.add(c.getCommitDate().atZone(ZoneOffset.UTC).toLocalDate());
                if (lastActivity == null || c.getCommitDate().isAfter(lastActivity)) {
                    lastActivity = c.getCommitDate();
                }
            }
        }

        ir.linesAdded    = totalAdded;
        ir.linesDeleted  = totalDeleted;
        ir.bugFixCommits = bugFixes;
        ir.activeDays    = activeDays.size();
        ir.lastActivity  = lastActivity;
        ir.codeChurnRate = MetricsCalculator.calcCodeChurnRate(totalAdded, totalDeleted);

        // Pillar 3: consistency multiplier
        ir.consistencyFactor = MetricsCalculator.calcConsistencyFactor(ir.activeDays);
        ir.rawGithubScore    = rawGit * ir.consistencyFactor;
        ir.feRawScore        = feRaw  * ir.consistencyFactor;
        ir.beRawScore        = beRaw  * ir.consistencyFactor;

        // ── Jira ───────────────────────────────────────────────────────────

        List<JiraIssue> myIssues = issuesByAssignee.getOrDefault(authorId, List.of());
        int assigned   = myIssues.size();
        int completed  = (int) myIssues.stream()
                .filter(i -> "Done".equalsIgnoreCase(i.getStatus())).count();
        int inProgress = (int) myIssues.stream()
                .filter(i -> "In Progress".equalsIgnoreCase(i.getStatus())).count();

        ir.tasksAssigned      = assigned;
        ir.tasksCompleted     = completed;
        ir.tasksInProgress    = inProgress;
        ir.taskCompletionRate = assigned > 0 ? (double) completed / assigned * 100.0 : 0.0;
        ir.hasOverdueTasks    = myIssues.stream().anyMatch(i ->
                i.getDueDate() != null
                && i.getDueDate().isBefore(LocalDate.now(ZoneOffset.UTC))
                && !"Done".equalsIgnoreCase(i.getStatus()));

        // Overdue task count — tasks past due date and not completed (used as penalty)
        ir.overdueTaskCount = (int) myIssues.stream()
                .filter(i -> i.getDueDate() != null
                        && i.getDueDate().isBefore(LocalDate.now(ZoneOffset.UTC))
                        && !"Done".equalsIgnoreCase(i.getStatus()))
                .count();

        // Smart Coder Bonus — rewards efficiency (tasks / log10 lines)
        ir.smartCoderBonus = MetricsCalculator.calcSmartCoderBonus(completed, totalAdded);
        // Use overdueTaskCount as quality penalty (trễ hạn bị trừ điểm)
        double jiraQuality = MetricsCalculator.calcJiraQualityFactor(ir.overdueTaskCount);
        ir.rawJiraScore    = (assigned > 0 ? (double) completed / assigned : 0.0)
                             * jiraQuality
                             * ir.smartCoderBonus;

        return ir;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build entity
    // ─────────────────────────────────────────────────────────────────────────

    private ContributionMetric buildMetric(IntermediateResult ir) {
        double finalScore = 0.5 * ir.githubImpactScore + 0.5 * ir.jiraExecutionScore;

        return ContributionMetric.builder()
                .projectId(ir.projectId)
                .userId(ir.user.getId())
                .domain(ir.primaryDomain != null ? ir.primaryDomain : DOMAIN_UNKNOWN)
                .totalCommits(ir.totalCommits)
                .linesAdded(ir.linesAdded)
                .linesDeleted(ir.linesDeleted)
                .bugFixCommits(ir.bugFixCommits)
                .activeDays(ir.activeDays)
                .consistencyFactor(ir.consistencyFactor)
                .codeChurnRate(round2(ir.codeChurnRate))
                .tasksAssigned(ir.tasksAssigned)
                .tasksCompleted(ir.tasksCompleted)
                .tasksInProgress(ir.tasksInProgress)
                .taskCompletionRate(round2(ir.taskCompletionRate))
                .overdueTaskCount(ir.overdueTaskCount)
                .smartCoderBonus(round2(ir.smartCoderBonus))
                .githubImpactScore(round2(ir.githubImpactScore))
                .jiraExecutionScore(round2(ir.jiraExecutionScore))
                .contributionScore(round2(finalScore))
                .lastActivityDate(ir.lastActivity)
                .inactive(isInactive(ir.lastActivity))
                .hasLowContribution(false)   // set by ContributionService after normalization
                .hasOverdueTasks(ir.hasOverdueTasks)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isInactive(Instant lastActivity) {
        if (lastActivity == null) return true;
        long daysSince = (Instant.now().toEpochMilli() - lastActivity.toEpochMilli())
                / (1000L * 60 * 60 * 24);
        return daysSince >= MetricsCalculator.INACTIVE_DAYS_THRESHOLD;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Intermediate computation state — not an entity
    // ─────────────────────────────────────────────────────────────────────────

    private static class IntermediateResult {
        Integer projectId;
        User    user;
        String  primaryDomain;

        // GitHub
        int    totalCommits, linesAdded, linesDeleted, bugFixCommits, activeDays;
        double consistencyFactor, codeChurnRate;
        double rawGithubScore, feRawScore, beRawScore;
        double githubImpactScore;   // set during normalization
        double smartCoderBonus;
        Instant lastActivity;

        // Jira
        int    tasksAssigned, tasksCompleted, tasksInProgress, overdueTaskCount;
        double taskCompletionRate, rawJiraScore;
        double jiraExecutionScore;  // set during normalization
        boolean hasOverdueTasks;
    }
}
