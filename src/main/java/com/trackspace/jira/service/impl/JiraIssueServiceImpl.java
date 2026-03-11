package com.trackspace.jira.service.impl;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.IssueType;
import com.trackspace.jira.dto.JiraIssueRequest;
import com.trackspace.jira.dto.JiraIssueResponse;
import com.trackspace.jira.dto.JiraSyncRequest;
import com.trackspace.jira.entity.JiraConnection;
import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.entity.JiraSprint;
import com.trackspace.jira.repository.JiraConnectionRepository;
import com.trackspace.jira.repository.JiraSprintRepository;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.jira.service.JiraApiClient;
import com.trackspace.jira.service.JiraApiClient.JiraIssueDto;
import com.trackspace.jira.service.JiraIssueService;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of JiraIssueService
 * Handles issue syncing, creation, status updates, and assignment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraIssueServiceImpl implements JiraIssueService {

    private final JiraIssueRepository issueRepository;
    private final JiraConnectionRepository connectionRepository;
    private final JiraSprintRepository sprintRepository;
    private final JiraApiClient jiraApiClient;
    private final UserRepository userRepository;

    @Override
    public List<JiraIssueResponse> getIssues(Integer projectId, Integer sprintId,
            String status, Integer assigneeId) {
        log.debug("Getting issues for project {}, sprint={}, status={}, assignee={}",
                projectId, sprintId, status, assigneeId);

        List<JiraIssue> issues = issueRepository.findByFilters(projectId, sprintId, status, assigneeId);

        return issues.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> syncIssues(JiraSyncRequest request) {
        log.info("Starting issue sync for project {}", request.getProjectId());

        // Get connection
        JiraConnection connection = connectionRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + request.getProjectId()));

        String siteUrl = connection.getSiteUrl();
        String email = connection.getEmail();
        String token = connection.getApiTokenEncrypted();
        String projectKey = connection.getProjectKey();
        Integer projectId = request.getProjectId();

        // === PARALLEL: Fetch issues AND sprint mappings simultaneously ===
        var issuesFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                jiraApiClient.fetchIssues(siteUrl, email, token, projectKey));

        // Fetch sprint-issue mappings for ALL sprints in parallel
        List<JiraSprint> localSprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
        Map<String, List<String>> sprintIssueMap = new java.util.concurrent.ConcurrentHashMap<>();
        var sprintFutures = localSprints.stream()
                .map(sprint -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        int jiraSprintIdInt = Integer.parseInt(sprint.getJiraSprintId());
                        List<String> issueKeys = jiraApiClient.fetchIssueKeysForSprint(
                                siteUrl, email, token, jiraSprintIdInt);
                        sprintIssueMap.put(String.valueOf(sprint.getId()), issueKeys);
                    } catch (Exception e) {
                        log.debug("Sprint fetch failed for {}: {}", sprint.getSprintName(), e.getMessage());
                    }
                }))
                .collect(Collectors.toList());

        // Wait for ALL to complete (issues + all sprint fetches)
        List<JiraIssueDto> jiraIssues;
        try {
            var allSprintsFuture = java.util.concurrent.CompletableFuture.allOf(
                    sprintFutures.toArray(new java.util.concurrent.CompletableFuture[0]));
            java.util.concurrent.CompletableFuture.allOf(issuesFuture, allSprintsFuture)
                    .get(60, java.util.concurrent.TimeUnit.SECONDS);
            jiraIssues = issuesFuture.get();
        } catch (Exception e) {
            log.warn("Parallel fetch failed, falling back: {}", e.getMessage());
            jiraIssues = jiraApiClient.fetchIssues(siteUrl, email, token, projectKey);
        }

        // Build reverse map: issueKey → sprintId (local)
        Map<String, Integer> issueKeyToSprintId = new java.util.HashMap<>();
        for (JiraSprint sprint : localSprints) {
            List<String> keys = sprintIssueMap.get(String.valueOf(sprint.getId()));
            if (keys != null) {
                for (String key : keys) {
                    issueKeyToSprintId.put(key, sprint.getId());
                }
            }
        }

        // Save issues with sprint mapping already resolved
        int syncedCount = 0;
        int updatedCount = 0;
        for (JiraIssueDto jiraIssue : jiraIssues) {
            var existingIssue = issueRepository.findByJiraIssueId(jiraIssue.getId());

            if (existingIssue.isPresent()) {
                JiraIssue issue = existingIssue.get();
                updateIssueFromDto(issue, jiraIssue);
                // Override sprint from Agile API (more reliable)
                if (issueKeyToSprintId.containsKey(issue.getIssueKey())) {
                    issue.setSprintId(issueKeyToSprintId.get(issue.getIssueKey()));
                }
                issueRepository.save(issue);
                updatedCount++;
            } else {
                JiraIssue issue = new JiraIssue();
                issue.setProjectId(projectId);
                issue.setJiraIssueId(jiraIssue.getId());
                issue.setIssueKey(jiraIssue.getKey());
                updateIssueFromDto(issue, jiraIssue);
                // Override sprint from Agile API (more reliable)
                if (issueKeyToSprintId.containsKey(jiraIssue.getKey())) {
                    issue.setSprintId(issueKeyToSprintId.get(jiraIssue.getKey()));
                }
                issueRepository.save(issue);
                syncedCount++;
            }
        }

        // Update connection lastSyncAt
        connection.setLastSyncAt(Instant.now());
        connectionRepository.save(connection);

        log.info("Issue sync completed for project {}: {} new, {} updated, {} sprints mapped",
                projectId, syncedCount, updatedCount, issueKeyToSprintId.size());

        return Map.of(
                "issuesSynced", syncedCount,
                "issuesUpdated", updatedCount,
                "lastSyncAt", Instant.now(),
                "message", String.format("Successfully synced %d new issues, updated %d",
                        syncedCount, updatedCount));
    }

    @Override
    @Transactional
    public JiraIssueResponse createIssue(JiraIssueRequest request) {
        log.info("Creating issue for project {}: {}", request.getProjectId(), request.getSummary());

        // Get connection
        JiraConnection connection = connectionRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + request.getProjectId()));

        // Create issue in Jira
        JiraIssueDto jiraIssue = jiraApiClient.createIssue(
                connection.getSiteUrl(), connection.getEmail(),
                connection.getApiTokenEncrypted(), connection.getProjectKey(),
                request.getIssueType(), request.getSummary(),
                request.getDescription(), request.getPriority());

        if (jiraIssue == null) {
            throw new BadRequestException("Failed to create issue in Jira");
        }

        // Save issue locally
        JiraIssue issue = new JiraIssue();
        issue.setProjectId(request.getProjectId());
        issue.setSprintId(request.getSprintId());
        issue.setJiraIssueId(jiraIssue.getId());
        issue.setIssueKey(jiraIssue.getKey());
        issue.setIssueType(parseIssueType(request.getIssueType()));
        issue.setSummary(request.getSummary());
        issue.setDescription(request.getDescription());
        issue.setStatus("To Do");
        issue.setPriority(request.getPriority());
        issue.setAssigneeId(request.getAssigneeId());
        issue.setDueDate(request.getDueDate());

        JiraIssue saved = issueRepository.save(issue);
        log.info("Successfully created issue {} in Jira and saved locally", jiraIssue.getKey());

        // Move issue to sprint on Jira if sprintId is provided
        if (request.getSprintId() != null) {
            JiraSprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElse(null);
            if (sprint != null && sprint.getJiraSprintId() != null) {
                try {
                    jiraApiClient.moveIssueToSprint(
                            connection.getSiteUrl(), connection.getEmail(),
                            connection.getApiTokenEncrypted(),
                            sprint.getJiraSprintId(), jiraIssue.getKey());
                    log.info("Moved issue {} to sprint {} on Jira", jiraIssue.getKey(), sprint.getSprintName());
                } catch (Exception ex) {
                    log.warn("Issue created but failed to move to sprint: {}", ex.getMessage());
                }
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JiraIssueResponse updateIssueStatus(Integer issueId, String newStatus) {
        log.info("Updating issue {} status to {}", issueId, newStatus);

        JiraIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        // Update in Jira
        JiraConnection connection = connectionRepository.findByProjectId(issue.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + issue.getProjectId()));

        jiraApiClient.updateIssueStatus(
                connection.getSiteUrl(), connection.getEmail(),
                connection.getApiTokenEncrypted(),
                issue.getIssueKey(), newStatus);

        // Update locally
        issue.setStatus(newStatus);
        JiraIssue saved = issueRepository.save(issue);

        log.info("Successfully updated issue {} status to {}", issue.getIssueKey(), newStatus);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public JiraIssueResponse assignIssue(Integer issueId, Integer assigneeId) {
        log.info("Assigning issue {} to user {}", issueId, assigneeId);

        JiraIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        issue.setAssigneeId(assigneeId);
        JiraIssue saved = issueRepository.save(issue);

        log.info("Successfully assigned issue {} to user {}", issue.getIssueKey(), assigneeId);
        return mapToResponse(saved);
    }

    /**
     * Assign issue on Jira using jiraAccountId, then update local
     */
    @Transactional
    public JiraIssueResponse assignIssueOnJira(Integer issueId, String jiraAccountId, String displayName, Integer userId) {
        log.info("Assigning issue {} to Jira accountId {} ({}) userId={}", issueId, jiraAccountId, displayName, userId);

        JiraIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        JiraConnection conn = connectionRepository.findByProjectId(issue.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + issue.getProjectId()));

        // Assign on Jira
        jiraApiClient.assignIssueOnJira(
                conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                issue.getIssueKey(), jiraAccountId);

        // Update local identifiers
        issue.setJiraAccountId(jiraAccountId);
        issue.setAssigneeName(displayName);

        // Resolve TrackSpace assigneeId:
        // 1. Use userId from request if provided
        // 2. Fallback: match displayName against User.fullName
        if (userId != null) {
            issue.setAssigneeId(userId);
        } else if (displayName != null && !displayName.isBlank()) {
            userRepository.findFirstByFullNameIgnoreCase(displayName)
                    .ifPresent(u -> issue.setAssigneeId(u.getId().intValue()));
        }

        JiraIssue saved = issueRepository.save(issue);
        log.info("Successfully assigned issue {} to {} (assigneeId={}) on Jira",
                issue.getIssueKey(), displayName, saved.getAssigneeId());
        return mapToResponse(saved);
    }

    @Override
    public List<Map<String, String>> getAssignableUsers(Integer projectId) {
        JiraConnection conn = connectionRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + projectId));

        var users = jiraApiClient.getAssignableUsers(
                conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(), conn.getProjectKey());

        return users.stream().map(u -> {
            Map<String, String> m = new java.util.HashMap<>();
            m.put("accountId", u.getAccountId());
            m.put("displayName", u.getDisplayName());
            m.put("emailAddress", u.getEmailAddress() != null ? u.getEmailAddress() : "");
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    private void updateIssueFromDto(JiraIssue issue, JiraIssueDto dto) {
        if (dto.getFields() != null) {
            issue.setSummary(dto.getFields().getSummary());

            // Extract plain text from ADF description
            if (dto.getFields().getDescription() != null) {
                issue.setDescription(extractTextFromAdf(dto.getFields().getDescription()));
            }

            if (dto.getFields().getStatus() != null) {
                issue.setStatus(dto.getFields().getStatus().getName());
            }

            if (dto.getFields().getPriority() != null) {
                issue.setPriority(dto.getFields().getPriority().getName());
            }

            if (dto.getFields().getIssuetype() != null) {
                issue.setIssueType(parseIssueType(dto.getFields().getIssuetype().getName()));
            }

            if (dto.getFields().getDuedate() != null && !dto.getFields().getDuedate().isEmpty()) {
                try {
                    issue.setDueDate(LocalDate.parse(dto.getFields().getDuedate()));
                } catch (Exception e) {
                    log.warn("Failed to parse due date: {}", dto.getFields().getDuedate());
                }
            }

            // Map sprint: lookup Jira sprint ID → local sprint_id
            if (dto.getFields().getSprint() != null && dto.getFields().getSprint().getId() != null) {
                String jiraSprintId = String.valueOf(dto.getFields().getSprint().getId());
                log.info("Issue {} has Jira sprint id={}, name={}",
                        dto.getKey(), jiraSprintId, dto.getFields().getSprint().getName());
                var localSprint = sprintRepository.findByJiraSprintId(jiraSprintId);
                if (localSprint.isPresent()) {
                    issue.setSprintId(localSprint.get().getId());
                    log.info("  → Mapped to local sprint id={}", localSprint.get().getId());
                } else {
                    log.warn("  → No local sprint found for jiraSprintId={}", jiraSprintId);
                    issue.setSprintId(null);
                }
            } else {
                log.info("Issue {} has no sprint field", dto.getKey());
                issue.setSprintId(null); // No sprint → backlog
            }

            // Map assignee displayName and accountId from Jira
            if (dto.getFields().getAssignee() != null) {
                String displayName = dto.getFields().getAssignee().getDisplayName();
                issue.setAssigneeName(displayName);
                issue.setJiraAccountId(dto.getFields().getAssignee().getAccountId());
                String assigneeEmail = dto.getFields().getAssignee().getEmailAddress();
                issue.setAssigneeEmail(assigneeEmail);
                // Strategy 1: Email match
                boolean matched = false;
                if (assigneeEmail != null && !assigneeEmail.isBlank()) {
                    var byEmail = userRepository.findByEmail(assigneeEmail);
                    if (byEmail.isPresent()) {
                        issue.setAssigneeId(byEmail.get().getId().intValue());
                        matched = true;
                    }
                }
                // Strategy 2: Full name match (fallback when Jira email is hidden)
                if (!matched && displayName != null && !displayName.isBlank()) {
                    userRepository.findFirstByFullNameIgnoreCase(displayName)
                            .ifPresent(u -> issue.setAssigneeId(u.getId().intValue()));
                }
            } else {
                issue.setAssigneeName(null);
                issue.setJiraAccountId(null);
                issue.setAssigneeEmail(null);
            }
        }
    }

    private IssueType parseIssueType(String type) {
        if (type == null)
            return IssueType.TASK;
        return switch (type.toUpperCase()) {
            case "EPIC" -> IssueType.EPIC;
            case "STORY" -> IssueType.STORY;
            case "BUG" -> IssueType.BUG;
            default -> IssueType.TASK;
        };
    }

    /**
     * Extract plain text from Jira ADF (Atlassian Document Format) description
     * ADF is a complex JSON structure; this provides basic text extraction
     */
    private String extractTextFromAdf(Object adf) {
        if (adf instanceof String) {
            return (String) adf;
        }
        if (adf instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = (Map<String, Object>) adf;
            StringBuilder sb = new StringBuilder();
            extractTextRecursive(doc, sb);
            return sb.toString().trim();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void extractTextRecursive(Map<String, Object> node, StringBuilder sb) {
        if ("text".equals(node.get("type"))) {
            Object text = node.get("text");
            if (text != null) {
                sb.append(text);
            }
        }
        Object content = node.get("content");
        if (content instanceof List) {
            for (Object child : (List<Object>) content) {
                if (child instanceof Map) {
                    extractTextRecursive((Map<String, Object>) child, sb);
                }
            }
        }
    }

    @Override
    @Transactional
    public JiraIssueResponse updateIssue(Integer issueId, JiraIssueRequest request) {
        JiraIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        JiraConnection conn = connectionRepository.findByProjectId(issue.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + issue.getProjectId()));

        // Update on Jira
        jiraApiClient.updateIssue(
                conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                issue.getIssueKey(),
                request.getSummary(), request.getDescription(),
                request.getPriority(), request.getIssueType(),
                request.getDueDate() != null ? request.getDueDate().toString() : null);

        // Update locally
        if (request.getSummary() != null)
            issue.setSummary(request.getSummary());
        if (request.getDescription() != null)
            issue.setDescription(request.getDescription());
        if (request.getPriority() != null)
            issue.setPriority(request.getPriority());
        if (request.getIssueType() != null)
            issue.setIssueType(parseIssueType(request.getIssueType()));
        if (request.getDueDate() != null)
            issue.setDueDate(request.getDueDate());

        // Handle sprint change (drag-and-drop between sprints)
        if (request.getSprintId() != null && !request.getSprintId().equals(issue.getSprintId())) {
            issue.setSprintId(request.getSprintId());
            // Move on Jira Cloud too
            JiraSprint targetSprint = sprintRepository.findById(request.getSprintId()).orElse(null);
            if (targetSprint != null && targetSprint.getJiraSprintId() != null) {
                try {
                    jiraApiClient.moveIssueToSprint(
                            conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                            targetSprint.getJiraSprintId(), issue.getIssueKey());
                    log.info("Moved issue {} to sprint {} on Jira", issue.getIssueKey(), targetSprint.getSprintName());
                } catch (Exception ex) {
                    log.warn("Failed to move issue {} to sprint on Jira: {}", issue.getIssueKey(), ex.getMessage());
                }
            }
        } else if (request.getSprintId() == null && issue.getSprintId() != null) {
            // Move to backlog (clear sprint)
            issue.setSprintId(null);
            log.info("Moved issue {} to backlog", issue.getIssueKey());
        }

        issueRepository.save(issue);

        log.info("Updated issue {}", issue.getIssueKey());
        return mapToResponse(issue);
    }

    @Override
    public void deleteIssue(Integer issueId) {
        JiraIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        JiraConnection conn = connectionRepository.findByProjectId(issue.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + issue.getProjectId()));

        // Delete locally first (guaranteed to succeed)
        issueRepository.delete(issue);
        log.info("Deleted issue {} from local DB", issue.getIssueKey());

        // Then delete on Jira (best-effort)
        try {
            jiraApiClient.deleteIssue(
                    conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                    issue.getIssueKey());
            log.info("Deleted issue {} from Jira", issue.getIssueKey());
        } catch (Exception e) {
            log.warn("Failed to delete issue {} on Jira (may already be deleted): {}", issue.getIssueKey(), e.getMessage());
        }
    }

    private JiraIssueResponse mapToResponse(JiraIssue issue) {
        return JiraIssueResponse.builder()
                .issueId(issue.getId())
                .projectId(issue.getProjectId())
                .sprintId(issue.getSprintId())
                .jiraIssueId(issue.getJiraIssueId())
                .issueKey(issue.getIssueKey())
                .issueType(issue.getIssueType())
                .summary(issue.getSummary())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .priority(issue.getPriority())
                .assigneeId(issue.getAssigneeId())
                .assigneeName(issue.getAssigneeName())
                .assigneeEmail(issue.getAssigneeEmail())
                .jiraAccountId(issue.getJiraAccountId())
                .dueDate(issue.getDueDate())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
