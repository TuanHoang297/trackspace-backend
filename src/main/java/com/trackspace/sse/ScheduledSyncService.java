package com.trackspace.sse;

import com.trackspace.github.ConnectionStatus;
import com.trackspace.github.dto.SyncRequest;
import com.trackspace.github.entity.Connection;
import com.trackspace.github.repository.ConnectionRepository;
import com.trackspace.github.service.CommitService;
import com.trackspace.jira.JiraConnectionStatus;
import com.trackspace.jira.dto.JiraSyncRequest;
import com.trackspace.jira.entity.JiraConnection;
import com.trackspace.jira.repository.JiraConnectionRepository;
import com.trackspace.jira.service.JiraIssueService;
import com.trackspace.jira.service.JiraSprintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled service that polls GitHub/Jira APIs every 30 seconds,
 * but ONLY for projects that have an active viewer (heartbeat within last 60s).
 * Frontend sends heartbeat when user is on GitHub/Jira pages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSyncService {

    private final CommitService commitService;
    private final JiraIssueService jiraIssueService;
    private final JiraSprintService jiraSprintService;
    private final ConnectionRepository githubConnectionRepository;
    private final JiraConnectionRepository jiraConnectionRepository;

    /** projectId → last heartbeat timestamp */
    private final Map<Integer, Instant> activeProjects = new ConcurrentHashMap<>();

    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(60);

    /** Called by frontend to mark a project as actively viewed. */
    public void heartbeat(Integer projectId) {
        activeProjects.put(projectId, Instant.now());
    }

    /** Called when frontend leaves the page. */
    public void deactivate(Integer projectId) {
        activeProjects.remove(projectId);
    }

    @Scheduled(fixedRate = 30_000, initialDelay = 10_000)
    public void pollForChanges() {
        // Clean expired heartbeats
        Instant cutoff = Instant.now().minus(HEARTBEAT_TTL);
        activeProjects.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));

        if (activeProjects.isEmpty()) return;

        Set<Integer> projectIds = new HashSet<>(activeProjects.keySet());
        log.debug("[Scheduler] Syncing {} active projects: {}", projectIds.size(), projectIds);

        for (Integer projectId : projectIds) {
            try {
                syncGitHub(projectId);
            } catch (Exception e) {
                log.warn("[Scheduler] GitHub sync failed for project {}: {}", projectId, e.getMessage());
            }

            try {
                syncJira(projectId);
            } catch (Exception e) {
                log.warn("[Scheduler] Jira sync failed for project {}: {}", projectId, e.getMessage());
            }
        }
    }

    private void syncGitHub(Integer projectId) {
        List<Connection> connections = githubConnectionRepository.findByProjectId(projectId);
        boolean hasConnected = connections.stream()
                .anyMatch(c -> c.getStatus() == ConnectionStatus.CONNECTED);
        if (!hasConnected) return;

        SyncRequest request = new SyncRequest();
        request.setProjectId(projectId);
        request.setSince(Instant.now().minus(Duration.ofMinutes(2)));

        Map<String, Object> result = commitService.syncCommits(request);
        int commitsSynced = (int) result.getOrDefault("commitsSynced", 0);

        if (commitsSynced > 0) {
            log.info("[Scheduler] GitHub: {} new commits for project {}", commitsSynced, projectId);
        }
    }

    private void syncJira(Integer projectId) {
        Optional<JiraConnection> jiraConn = jiraConnectionRepository.findByProjectId(projectId);
        if (jiraConn.isEmpty() || jiraConn.get().getConnectionStatus() != JiraConnectionStatus.CONNECTED) {
            return;
        }

        JiraSyncRequest request = new JiraSyncRequest(projectId);

        Map<String, Object> issueResult = jiraIssueService.syncIssues(request);
        int issuesSynced = (int) issueResult.getOrDefault("issuesSynced", 0);
        int issuesUpdated = (int) issueResult.getOrDefault("issuesUpdated", 0);

        Map<String, Object> sprintResult = jiraSprintService.syncSprints(request);
        int sprintsSynced = (int) sprintResult.getOrDefault("sprintsSynced", 0);
        int sprintsUpdated = (int) sprintResult.getOrDefault("sprintsUpdated", 0);

        int totalChanges = issuesSynced + issuesUpdated + sprintsSynced + sprintsUpdated;
        if (totalChanges > 0) {
            log.info("[Scheduler] Jira: {} changes for project {} (issues: +{}/~{}, sprints: +{}/~{})",
                    totalChanges, projectId, issuesSynced, issuesUpdated, sprintsSynced, sprintsUpdated);
        }
    }
}
