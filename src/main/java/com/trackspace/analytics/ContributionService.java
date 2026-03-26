package com.trackspace.analytics;

import com.trackspace.auth.AuthService;
import com.trackspace.classroom.GroupMember;
import com.trackspace.classroom.GroupMemberRepository;
import com.trackspace.common.ForbiddenException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.project.Project;
import com.trackspace.project.ProjectRepository;
import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestration service for the Analytics module.
 *
 * Entry points:
 *  - recalculate(projectId): fetch raw data → compute → persist
 *  - getByProject / getByUser: return cached ContributionResponse
 *  - getDashboard(projectId): aggregate DashboardResponse
 *  - getHeatmap(userId, projectId): activity HeatmapResponse
 *  - detectIssues(projectId): IssueDetectionResponse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionMetricRepository metricRepo;
    private final ContributionCalculator calculator;
    private final ProjectRepository projectRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final JiraIssueRepository jiraIssueRepository;
    private final com.trackspace.github.repository.CommitRepository commitRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────────────────
    // Access Control
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Throws ForbiddenException if the currently authenticated user is neither
     * a member of the project's group nor a LECTURER / ADMIN.
     */
    public void checkProjectAccess(Integer projectId) {
        var currentUser = authService.getCurrentUser();
        User.Role role = currentUser.getRole();
        if (role == User.Role.LECTURER || role == User.Role.ADMIN) return;

        Project project = projectRepository.findByIdAndDeletedFalse(projectId.longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        Long groupId = project.getGroup().getId();

        boolean isMember = groupMemberRepository
                .findByGroupIdWithMember(groupId)
                .stream()
                .anyMatch(gm -> gm.getMember().getId().equals(currentUser.getId()));

        if (!isMember) {
            throw new ForbiddenException("Bạn không có quyền xem dự án này");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recalculate & Persist
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * (Re)calculates all contribution metrics for a project and persists them.
     * Idempotent: upserts by (projectId, userId).
     * Uses default 50/50 domain weighting.
     */
    @Transactional
    public List<ContributionResponse> recalculate(Integer projectId) {
        return recalculate(projectId, 0.5, 0.5);
    }

    /**
     * (Re)calculates with explicit FRONTEND / BACKEND domain weights.
     *
     * @param feWeight weight for FRONTEND repo scores (0–1)
     * @param beWeight weight for BACKEND repo scores  (0–1)
     */
    @Transactional
    public List<ContributionResponse> recalculate(Integer projectId,
                                                   double feWeight, double beWeight) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId.longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        // Collect group members
        Long groupId = project.getGroup().getId();
        List<User> members = groupMemberRepository
                .findByGroupIdWithMember(groupId)
                .stream()
                .map(GroupMember::getMember)
                .collect(Collectors.toList());

        if (members.isEmpty()) {
            log.warn("Project {} has no group members — skipping analytics calculation", projectId);
            return List.of();
        }

        // Compute metrics with domain weights
        List<ContributionMetric> computed = calculator.calculate(projectId, members, feWeight, beWeight);

        // Apply low-contribution flag after normalisation
        double avg = computed.stream()
                .mapToDouble(ContributionMetric::getContributionScore)
                .average()
                .orElse(0.0);
        computed.forEach(m -> m.setHasLowContribution(
                m.getContributionScore() < avg * MetricsCalculator.LOW_CONTRIBUTION_RATIO));

        // Upsert
        for (ContributionMetric computed1 : computed) {
            metricRepo.findByProjectIdAndUserId(projectId, computed1.getUserId())
                    .ifPresentOrElse(
                            existing -> copyFields(existing, computed1),
                            () -> metricRepo.save(computed1));
        }
        metricRepo.flush();

        return toResponses(computed, buildUserMap(members));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query Methods
    // ─────────────────────────────────────────────────────────────────────────

    /** All members' contributions for a project (from cached metrics). */
    @Transactional
    public List<ContributionResponse> getByProject(Integer projectId) {
        List<ContributionMetric> metrics =
                metricRepo.findByProjectIdOrderByContributionScoreDesc(projectId);
        if (metrics.isEmpty()) return recalculate(projectId);
        Map<Long, User> userMap = buildUserMapFromMetrics(metrics);
        return toResponses(metrics, userMap);
    }

    /** Single user's contribution snapshot. Auto-triggers recalculation if missing. */
    @Transactional
    public ContributionResponse getByUser(Integer projectId, Long userId) {
        return metricRepo.findByProjectIdAndUserId(projectId, userId)
                .map(m -> toResponse(m, null))
                .orElseGet(() -> {
                    List<ContributionResponse> all = recalculate(projectId);
                    return all.stream()
                            .filter(r -> r.getUserId().equals(userId))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "User " + userId + " not found in project " + projectId));
                });
    }

    /** Full project dashboard with aggregates + per-member chart data. */
    @Transactional
    public DashboardResponse getDashboard(Integer projectId) {
        List<ContributionResponse> members = getByProject(projectId);

        int totalCommits    = members.stream().mapToInt(ContributionResponse::getTotalCommits).sum();
        int totalLinesAdded = members.stream().mapToInt(ContributionResponse::getLinesAdded).sum();
        int totalAssigned   = members.stream().mapToInt(ContributionResponse::getTasksAssigned).sum();
        int totalCompleted  = members.stream().mapToInt(ContributionResponse::getTasksCompleted).sum();
        double overallRate  = totalAssigned > 0
                ? (double) totalCompleted / totalAssigned * 100.0 : 0.0;

        // Jira status distribution
        Map<String, Long> statusDist = jiraIssueRepository.findByProjectId(projectId).stream()
                .filter(i -> i.getStatus() != null)
                .collect(Collectors.groupingBy(JiraIssue::getStatus, Collectors.counting()));

        // Anomaly messages
        List<String> anomalies = buildAnomalyMessages(members);

        return DashboardResponse.builder()
                .projectId(projectId)
                .totalMembers(members.size())
                .totalCommits(totalCommits)
                .totalLinesAdded(totalLinesAdded)
                .totalTasksAssigned(totalAssigned)
                .totalTasksCompleted(totalCompleted)
                .overallCompletionRate(Math.round(overallRate * 100.0) / 100.0)
                .issueStatusDistribution(statusDist)
                .memberContributions(members)
                .detectedAnomalies(anomalies)
                .build();
    }

    /** Activity heatmap for one user across a project. */
    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmap(Integer projectId, Long userId) {
        var commits = commitRepository.findByProjectIdAndAuthorId(projectId, userId.intValue());

        // Aggregate by date
        Map<LocalDate, int[]> byDay = new TreeMap<>();
        for (var c : commits) {
            if (c.getCommitDate() == null) continue;
            LocalDate day = c.getCommitDate().atZone(ZoneOffset.UTC).toLocalDate();
            byDay.computeIfAbsent(day, k -> new int[]{0, 0, 0});
            byDay.get(day)[0]++;                                             // commitCount
            byDay.get(day)[1] += c.getLinesAddedCode()   != null ? c.getLinesAddedCode()
                               : (c.getLinesAdded()   != null ? c.getLinesAdded()   : 0);
            byDay.get(day)[2] += c.getLinesDeletedCode() != null ? c.getLinesDeletedCode()
                               : (c.getLinesDeleted() != null ? c.getLinesDeleted() : 0);
        }

        List<HeatmapResponse.HeatmapEntry> entries = byDay.entrySet().stream()
                .map(e -> HeatmapResponse.HeatmapEntry.builder()
                        .date(e.getKey().toString())
                        .commitCount(e.getValue()[0])
                        .linesAdded(e.getValue()[1])
                        .linesDeleted(e.getValue()[2])
                        .build())
                .collect(Collectors.toList());

        int totalLinesAdded = commits.stream()
                .mapToInt(c -> c.getLinesAddedCode() != null ? c.getLinesAddedCode()
                             : (c.getLinesAdded() != null ? c.getLinesAdded() : 0)).sum();

        return HeatmapResponse.builder()
                .userId(userId)
                .projectId(projectId)
                .entries(entries)
                .totalActiveDays(byDay.size())
                .totalCommits(commits.size())
                .totalLinesAdded(totalLinesAdded)
                .build();
    }

    /** Detect and surface anomalies (inactive, low contribution, overdue, high churn). */
    @Transactional
    public IssueDetectionResponse detectIssues(Integer projectId) {
        List<ContributionResponse> members = getByProject(projectId);
        return IssueDetectionResponse.builder()
                .projectId(projectId)
                .issues(buildMemberIssues(members))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Copy computed fields onto an existing persisted entity (upsert helper). */
    private void copyFields(ContributionMetric target, ContributionMetric src) {
        target.setTotalCommits(src.getTotalCommits());
        target.setLinesAdded(src.getLinesAdded());
        target.setLinesDeleted(src.getLinesDeleted());
        target.setBugFixCommits(src.getBugFixCommits());
        target.setActiveDays(src.getActiveDays());
        target.setConsistencyFactor(src.getConsistencyFactor());
        target.setCodeChurnRate(src.getCodeChurnRate());
        target.setTasksAssigned(src.getTasksAssigned());
        target.setTasksCompleted(src.getTasksCompleted());
        target.setTasksInProgress(src.getTasksInProgress());
        target.setTaskCompletionRate(src.getTaskCompletionRate());
        target.setOverdueTaskCount(src.getOverdueTaskCount());
        target.setDomain(src.getDomain());
        target.setSmartCoderBonus(src.getSmartCoderBonus());
        target.setGithubImpactScore(src.getGithubImpactScore());
        target.setJiraExecutionScore(src.getJiraExecutionScore());
        target.setContributionScore(src.getContributionScore());
        target.setLastActivityDate(src.getLastActivityDate());
        target.setInactive(src.getInactive());
        target.setHasLowContribution(src.getHasLowContribution());
        target.setHasOverdueTasks(src.getHasOverdueTasks());
        target.setUpdatedAt(Instant.now());
        target.setCalculatedAt(Instant.now());
        metricRepo.save(target);
    }

    private List<ContributionResponse> toResponses(List<ContributionMetric> metrics,
                                                    Map<Long, User> userMap) {
        return metrics.stream()
                .map(m -> toResponse(m, userMap.get(m.getUserId())))
                .collect(Collectors.toList());
    }

    private ContributionResponse toResponse(ContributionMetric m, User user) {
        return ContributionResponse.builder()
                .userId(m.getUserId())
                .fullName(user != null ? user.getFullName() : "Unknown")
                .email(user != null ? user.getEmail() : null)
                .totalCommits(m.getTotalCommits()        != null ? m.getTotalCommits()        : 0)
                .linesAdded(m.getLinesAdded()            != null ? m.getLinesAdded()            : 0)
                .linesDeleted(m.getLinesDeleted()        != null ? m.getLinesDeleted()        : 0)
                .bugFixCommits(m.getBugFixCommits()      != null ? m.getBugFixCommits()      : 0)
                .githubImpactScore(m.getGithubImpactScore() != null ? m.getGithubImpactScore() : 0.0)
                .activeDays(m.getActiveDays()            != null ? m.getActiveDays()            : 0)
                .consistencyFactor(m.getConsistencyFactor() != null ? m.getConsistencyFactor() : 1.0)
                .tasksAssigned(m.getTasksAssigned()      != null ? m.getTasksAssigned()      : 0)
                .tasksCompleted(m.getTasksCompleted()    != null ? m.getTasksCompleted()    : 0)
                .tasksInProgress(m.getTasksInProgress()  != null ? m.getTasksInProgress()  : 0)
                .taskCompletionRate(m.getTaskCompletionRate() != null ? m.getTaskCompletionRate() : 0.0)
                .overdueTaskCount(m.getOverdueTaskCount() != null ? m.getOverdueTaskCount() : 0)
                .domain(m.getDomain())
                .smartCoderBonus(m.getSmartCoderBonus()  != null ? m.getSmartCoderBonus()  : 1.0)
                .jiraExecutionScore(m.getJiraExecutionScore() != null ? m.getJiraExecutionScore() : 0.0)
                .codeChurnRate(m.getCodeChurnRate()      != null ? m.getCodeChurnRate()      : 0.0)
                .contributionScore(m.getContributionScore() != null ? m.getContributionScore() : 0.0)
                .inactive(Boolean.TRUE.equals(m.getInactive()))
                .hasLowContribution(Boolean.TRUE.equals(m.getHasLowContribution()))
                .hasOverdueTasks(Boolean.TRUE.equals(m.getHasOverdueTasks()))
                .lastActivityDate(m.getLastActivityDate())
                .calculatedAt(m.getCalculatedAt())
                .build();
    }

    private Map<Long, User> buildUserMap(List<User> members) {
        return members.stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private Map<Long, User> buildUserMapFromMetrics(List<ContributionMetric> metrics) {
        List<Long> userIds = metrics.stream()
                .map(ContributionMetric::getUserId)
                .distinct()
                .collect(Collectors.toList());
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private List<IssueDetectionResponse.MemberIssue> buildMemberIssues(
            List<ContributionResponse> members) {
        List<IssueDetectionResponse.MemberIssue> issues = new ArrayList<>();
        for (ContributionResponse m : members) {
            if (m.isInactive())
                addIssue(issues, m, "INACTIVE",
                        m.getFullName() + " has not committed in ≥ "
                        + MetricsCalculator.INACTIVE_DAYS_THRESHOLD + " days");
            if (m.isHasLowContribution())
                addIssue(issues, m, "LOW_CONTRIBUTION",
                        m.getFullName() + " score (" + m.getContributionScore()
                        + ") is below 20% of project average");
            if (m.isHasOverdueTasks())
                addIssue(issues, m, "OVERDUE_TASKS",
                        m.getFullName() + " has Jira tasks past their due date");
            if (m.getCodeChurnRate() > MetricsCalculator.HIGH_CHURN_THRESHOLD)
                addIssue(issues, m, "HIGH_CHURN",
                        m.getFullName() + " code churn rate "
                        + String.format("%.2f", m.getCodeChurnRate())
                        + " (deleted > added — possible low-quality code)");
        }
        return issues;
    }

    private void addIssue(List<IssueDetectionResponse.MemberIssue> list,
                          ContributionResponse m, String type, String description) {
        list.add(IssueDetectionResponse.MemberIssue.builder()
                .userId(m.getUserId())
                .userName(m.getFullName())
                .issueType(type)
                .description(description)
                .currentScore(m.getContributionScore())
                .build());
    }

    private List<String> buildAnomalyMessages(List<ContributionResponse> members) {
        return buildMemberIssues(members).stream()
                .map(i -> "[" + i.getIssueType() + "] " + i.getDescription())
                .collect(Collectors.toList());
    }
}
