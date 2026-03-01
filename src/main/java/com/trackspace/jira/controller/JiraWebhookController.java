package com.trackspace.jira.controller;

import com.trackspace.jira.SprintStatus;
import com.trackspace.jira.entity.JiraConnection;
import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.entity.JiraSprint;
import com.trackspace.jira.repository.JiraConnectionRepository;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.jira.repository.JiraSprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Jira Webhook receiver — handles real-time events from Jira Cloud.
 * No authentication required (Jira sends POST with event payload).
 * 
 * Setup on Jira: Settings → System → Webhooks → Create
 * URL: https://your-backend.com/api/v1/jira/webhook
 * Events: Issue (created, updated, deleted) + Sprint (created, updated,
 * deleted, started, closed)
 */
@RestController
@RequestMapping("/api/v1/jira/webhook")
@RequiredArgsConstructor
@Slf4j
public class JiraWebhookController {

    private final JiraIssueRepository issueRepository;
    private final JiraSprintRepository sprintRepository;
    private final JiraConnectionRepository connectionRepository;

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        String webhookEvent = (String) payload.get("webhookEvent");
        log.info("Received Jira webhook event: {}", webhookEvent);

        if (webhookEvent == null) {
            return ResponseEntity.ok("ignored");
        }

        try {
            switch (webhookEvent) {
                case "jira:issue_created" -> handleIssueCreated(payload);
                case "jira:issue_updated" -> handleIssueUpdated(payload);
                case "jira:issue_deleted" -> handleIssueDeleted(payload);
                case "sprint_created" -> handleSprintCreatedOrUpdated(payload);
                case "sprint_updated", "sprint_started", "sprint_closed" -> handleSprintCreatedOrUpdated(payload);
                case "sprint_deleted" -> handleSprintDeleted(payload);
                default -> log.debug("Ignoring webhook event: {}", webhookEvent);
            }
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("ok");
    }

    @SuppressWarnings("unchecked")
    private void handleIssueCreated(Map<String, Object> payload) {
        Map<String, Object> issueData = (Map<String, Object>) payload.get("issue");
        if (issueData == null)
            return;

        String issueId = String.valueOf(issueData.get("id"));
        String issueKey = (String) issueData.get("key");

        // Check if already exists
        if (issueRepository.findByJiraIssueId(issueId).isPresent()) {
            log.debug("Issue {} already exists, treating as update", issueKey);
            handleIssueUpdated(payload);
            return;
        }

        // Find which project this belongs to by matching project key
        Map<String, Object> fields = (Map<String, Object>) issueData.get("fields");
        if (fields == null)
            return;

        Map<String, Object> project = (Map<String, Object>) fields.get("project");
        if (project == null)
            return;
        String projectKey = (String) project.get("key");

        // Find connection by project key to get our projectId
        Optional<JiraConnection> conn = connectionRepository.findByProjectKey(projectKey);
        if (conn.isEmpty()) {
            log.warn("No TrackSpace project connected for Jira project key: {}", projectKey);
            return;
        }

        JiraIssue issue = new JiraIssue();
        issue.setProjectId(conn.get().getProjectId());
        issue.setJiraIssueId(issueId);
        issue.setIssueKey(issueKey);
        applyFieldsToIssue(issue, fields);
        issueRepository.save(issue);

        log.info("Webhook: Created issue {} for project {}", issueKey, conn.get().getProjectId());
    }

    @SuppressWarnings("unchecked")
    private void handleIssueUpdated(Map<String, Object> payload) {
        Map<String, Object> issueData = (Map<String, Object>) payload.get("issue");
        if (issueData == null)
            return;

        String issueId = String.valueOf(issueData.get("id"));
        String issueKey = (String) issueData.get("key");

        Optional<JiraIssue> existing = issueRepository.findByJiraIssueId(issueId);
        if (existing.isEmpty()) {
            log.debug("Issue {} not found locally, treating as create", issueKey);
            handleIssueCreated(payload);
            return;
        }

        JiraIssue issue = existing.get();
        Map<String, Object> fields = (Map<String, Object>) issueData.get("fields");
        if (fields != null) {
            applyFieldsToIssue(issue, fields);
            issueRepository.save(issue);
            log.info("Webhook: Updated issue {}", issueKey);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleIssueDeleted(Map<String, Object> payload) {
        Map<String, Object> issueData = (Map<String, Object>) payload.get("issue");
        if (issueData == null)
            return;

        String issueId = String.valueOf(issueData.get("id"));
        issueRepository.findByJiraIssueId(issueId).ifPresent(issue -> {
            issueRepository.delete(issue);
            log.info("Webhook: Deleted issue {} (jiraId={})", issue.getIssueKey(), issueId);
        });
    }

    @SuppressWarnings("unchecked")
    private void applyFieldsToIssue(JiraIssue issue, Map<String, Object> fields) {
        // Summary
        if (fields.get("summary") != null) {
            issue.setSummary((String) fields.get("summary"));
        }

        // Status
        Map<String, Object> status = (Map<String, Object>) fields.get("status");
        if (status != null) {
            issue.setStatus((String) status.get("name"));
        }

        // Priority
        Map<String, Object> priority = (Map<String, Object>) fields.get("priority");
        if (priority != null) {
            issue.setPriority((String) priority.get("name"));
        }

        // Issue type
        Map<String, Object> issuetype = (Map<String, Object>) fields.get("issuetype");
        if (issuetype != null) {
            String typeName = (String) issuetype.get("name");
            issue.setIssueType(parseIssueType(typeName));
        }

        // Sprint mapping
        Map<String, Object> sprint = (Map<String, Object>) fields.get("sprint");
        if (sprint != null && sprint.get("id") != null) {
            String jiraSprintId = String.valueOf(sprint.get("id"));
            sprintRepository.findByJiraSprintId(jiraSprintId).ifPresent(
                    localSprint -> issue.setSprintId(localSprint.getId()));
        }
    }

    // ==================== SPRINT HANDLERS ====================

    @SuppressWarnings("unchecked")
    private void handleSprintCreatedOrUpdated(Map<String, Object> payload) {
        Map<String, Object> sprintData = (Map<String, Object>) payload.get("sprint");
        if (sprintData == null)
            return;

        String jiraSprintId = String.valueOf(sprintData.get("id"));
        String name = (String) sprintData.get("name");
        String state = (String) sprintData.get("state");
        String goal = (String) sprintData.get("goal");
        String startDate = (String) sprintData.get("startDate");
        String endDate = (String) sprintData.get("endDate");

        // Find or create sprint
        JiraSprint sprint = sprintRepository.findByJiraSprintId(jiraSprintId)
                .orElse(new JiraSprint());

        // If new sprint, need to find projectId from board/connection
        if (sprint.getId() == null) {
            sprint.setJiraSprintId(jiraSprintId);
            // Try to find projectId from originBoardId or fallback
            Integer projectId = findProjectIdFromPayload(payload);
            if (projectId == null) {
                log.warn("Cannot determine projectId for sprint {}", name);
                return;
            }
            sprint.setProjectId(projectId);
        }

        sprint.setSprintName(name != null ? name : "Sprint");
        sprint.setSprintGoal(goal);
        if (state != null) {
            sprint.setStatus(parseSprintStatus(state));
        }
        if (startDate != null && !startDate.isEmpty()) {
            sprint.setStartDate(parseDate(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            sprint.setEndDate(parseDate(endDate));
        }

        sprintRepository.save(sprint);
        log.info("Webhook: {} sprint '{}' (jiraId={})",
                sprint.getId() != null ? "Updated" : "Created", name, jiraSprintId);
    }

    @SuppressWarnings("unchecked")
    private void handleSprintDeleted(Map<String, Object> payload) {
        Map<String, Object> sprintData = (Map<String, Object>) payload.get("sprint");
        if (sprintData == null)
            return;

        String jiraSprintId = String.valueOf(sprintData.get("id"));
        sprintRepository.findByJiraSprintId(jiraSprintId).ifPresent(sprint -> {
            // Move issues from this sprint to backlog
            issueRepository.findByProjectIdAndSprintId(sprint.getProjectId(), sprint.getId())
                    .forEach(issue -> {
                        issue.setSprintId(null);
                        issueRepository.save(issue);
                    });
            sprintRepository.delete(sprint);
            log.info("Webhook: Deleted sprint '{}' (jiraId={})", sprint.getSprintName(), jiraSprintId);
        });
    }

    @SuppressWarnings("unchecked")
    private Integer findProjectIdFromPayload(Map<String, Object> payload) {
        // Sprint webhook may not have project info directly
        // Try to get from any existing connection (first match)
        var connections = connectionRepository.findByConnectionStatus(
                com.trackspace.jira.JiraConnectionStatus.CONNECTED);
        if (!connections.isEmpty()) {
            return connections.get(0).getProjectId();
        }
        return null;
    }

    // ==================== HELPERS ====================

    private com.trackspace.jira.IssueType parseIssueType(String type) {
        if (type == null)
            return com.trackspace.jira.IssueType.TASK;
        return switch (type.toUpperCase()) {
            case "EPIC" -> com.trackspace.jira.IssueType.EPIC;
            case "STORY" -> com.trackspace.jira.IssueType.STORY;
            case "BUG" -> com.trackspace.jira.IssueType.BUG;
            default -> com.trackspace.jira.IssueType.TASK;
        };
    }

    private SprintStatus parseSprintStatus(String state) {
        if (state == null)
            return SprintStatus.FUTURE;
        return switch (state.toLowerCase()) {
            case "active" -> SprintStatus.ACTIVE;
            case "closed" -> SprintStatus.CLOSED;
            default -> SprintStatus.FUTURE;
        };
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr.contains("T")) {
                return LocalDate.parse(dateStr.substring(0, 10));
            }
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}
