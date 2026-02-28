package com.trackspace.jira.service.impl;

import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.SprintStatus;
import com.trackspace.jira.dto.JiraSprintResponse;
import com.trackspace.jira.dto.JiraSyncRequest;
import com.trackspace.jira.entity.JiraConnection;
import com.trackspace.jira.entity.JiraSprint;
import com.trackspace.jira.repository.JiraConnectionRepository;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.jira.repository.JiraSprintRepository;
import com.trackspace.jira.service.JiraApiClient;
import com.trackspace.jira.service.JiraApiClient.JiraSprintDto;
import com.trackspace.jira.service.JiraSprintService;
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
 * Implementation of JiraSprintService
 * Handles sprint syncing and retrieval
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraSprintServiceImpl implements JiraSprintService {

    private final JiraSprintRepository sprintRepository;
    private final JiraIssueRepository issueRepository;
    private final JiraConnectionRepository connectionRepository;
    private final JiraApiClient jiraApiClient;

    @Override
    public List<JiraSprintResponse> getSprints(Integer projectId) {
        log.debug("Getting sprints for project {}", projectId);

        List<JiraSprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);

        return sprints.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> syncSprints(JiraSyncRequest request) {
        log.info("Starting sprint sync for project {}", request.getProjectId());

        // Get connection
        JiraConnection connection = connectionRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + request.getProjectId()));

        // Fetch sprints from Jira
        List<JiraSprintDto> jiraSprints = jiraApiClient.fetchSprints(
                connection.getSiteUrl(), connection.getEmail(),
                connection.getApiTokenEncrypted(), connection.getProjectKey());

        int syncedCount = 0;
        int updatedCount = 0;

        for (JiraSprintDto jiraSprint : jiraSprints) {
            String externalId = String.valueOf(jiraSprint.getId());

            // Check if sprint exists
            var existingSprint = sprintRepository.findByJiraSprintId(externalId);

            if (existingSprint.isPresent()) {
                // Update existing sprint
                JiraSprint sprint = existingSprint.get();
                updateSprintFromDto(sprint, jiraSprint);
                sprintRepository.save(sprint);
                updatedCount++;
            } else {
                // Create new sprint
                JiraSprint sprint = new JiraSprint();
                sprint.setProjectId(request.getProjectId());
                sprint.setJiraSprintId(externalId);
                updateSprintFromDto(sprint, jiraSprint);
                sprintRepository.save(sprint);
                syncedCount++;
            }
        }

        log.info("Sprint sync completed for project {}: {} new, {} updated",
                request.getProjectId(), syncedCount, updatedCount);

        return Map.of(
                "sprintsSynced", syncedCount,
                "sprintsUpdated", updatedCount,
                "message", String.format("Successfully synced %d new sprints, updated %d",
                        syncedCount, updatedCount));
    }

    private void updateSprintFromDto(JiraSprint sprint, JiraSprintDto dto) {
        sprint.setSprintName(dto.getName());
        sprint.setSprintGoal(dto.getGoal());

        // Parse status
        if (dto.getState() != null) {
            sprint.setStatus(parseSprintStatus(dto.getState()));
        }

        // Parse dates
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            sprint.setStartDate(parseLocalDate(dto.getStartDate()));
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            sprint.setEndDate(parseLocalDate(dto.getEndDate()));
        }
    }

    private SprintStatus parseSprintStatus(String state) {
        return switch (state.toLowerCase()) {
            case "active" -> SprintStatus.ACTIVE;
            case "closed" -> SprintStatus.CLOSED;
            default -> SprintStatus.FUTURE;
        };
    }

    private LocalDate parseLocalDate(String dateStr) {
        try {
            // Jira dates can be ISO-8601 with time, extract just the date part
            if (dateStr.contains("T")) {
                return LocalDate.parse(dateStr.substring(0, 10));
            }
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    private JiraSprintResponse mapToResponse(JiraSprint sprint) {
        long totalIssues = issueRepository.findByProjectIdAndSprintId(
                sprint.getProjectId(), sprint.getId()).size();
        long doneIssues = issueRepository.findByProjectIdAndSprintId(
                sprint.getProjectId(), sprint.getId()).stream()
                .filter(i -> "Done".equalsIgnoreCase(i.getStatus()))
                .count();

        return JiraSprintResponse.builder()
                .sprintId(sprint.getId())
                .projectId(sprint.getProjectId())
                .jiraSprintId(sprint.getJiraSprintId())
                .sprintName(sprint.getSprintName())
                .sprintGoal(sprint.getSprintGoal())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .totalIssues(totalIssues)
                .doneIssues(doneIssues)
                .build();
    }
}
