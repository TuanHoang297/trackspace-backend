package com.trackspace.jira.service.impl;

import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.SprintStatus;
import com.trackspace.jira.dto.JiraSprintRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return sprintRepository.findByProjectIdOrderByStartDateAsc(projectId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> syncSprints(JiraSyncRequest request) {
        JiraConnection conn = getConnection(request.getProjectId());
        List<JiraSprintDto> jiraSprints = jiraApiClient.fetchSprints(
                conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(), conn.getProjectKey());

        int synced = 0, updated = 0;
        for (JiraSprintDto dto : jiraSprints) {
            String externalId = String.valueOf(dto.getId());
            var existing = sprintRepository.findByJiraSprintId(externalId);
            if (existing.isPresent()) {
                updateSprintFromDto(existing.get(), dto);
                sprintRepository.save(existing.get());
                updated++;
            } else {
                JiraSprint sprint = new JiraSprint();
                sprint.setProjectId(request.getProjectId());
                sprint.setJiraSprintId(externalId);
                updateSprintFromDto(sprint, dto);
                sprintRepository.save(sprint);
                synced++;
            }
        }
        return Map.of("sprintsSynced", synced, "sprintsUpdated", updated);
    }

    @Override
    @Transactional
    public JiraSprintResponse createSprint(JiraSprintRequest request) {
        JiraConnection conn = getConnection(request.getProjectId());

        // Create on Jira first
        JiraSprintDto jiraSprint = jiraApiClient.createSprint(
                conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                conn.getProjectKey(), request.getName(),
                request.getStartDate(), request.getEndDate(), request.getGoal());

        // Save locally
        JiraSprint sprint = new JiraSprint();
        sprint.setProjectId(request.getProjectId());
        sprint.setJiraSprintId(String.valueOf(jiraSprint.getId()));
        sprint.setSprintName(request.getName());
        sprint.setSprintGoal(request.getGoal());
        sprint.setStatus(SprintStatus.FUTURE);
        if (request.getStartDate() != null)
            sprint.setStartDate(parseLocalDate(request.getStartDate()));
        if (request.getEndDate() != null)
            sprint.setEndDate(parseLocalDate(request.getEndDate()));
        sprintRepository.save(sprint);

        log.info("Created sprint '{}' jiraId={}", sprint.getSprintName(), sprint.getJiraSprintId());
        return mapToResponse(sprint);
    }

    @Override
    @Transactional
    public JiraSprintResponse updateSprint(Integer sprintId, JiraSprintRequest request) {
        JiraSprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));
        JiraConnection conn = getConnection(sprint.getProjectId());

        jiraApiClient.updateSprint(conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                Integer.parseInt(sprint.getJiraSprintId()),
                request.getName(), request.getStartDate(), request.getEndDate(), request.getGoal(), null);

        if (request.getName() != null)
            sprint.setSprintName(request.getName());
        if (request.getGoal() != null)
            sprint.setSprintGoal(request.getGoal());
        if (request.getStartDate() != null)
            sprint.setStartDate(parseLocalDate(request.getStartDate()));
        if (request.getEndDate() != null)
            sprint.setEndDate(parseLocalDate(request.getEndDate()));
        sprintRepository.save(sprint);
        return mapToResponse(sprint);
    }

    @Override
    @Transactional
    public void deleteSprint(Integer sprintId) {
        JiraSprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));
        JiraConnection conn = getConnection(sprint.getProjectId());

        jiraApiClient.deleteSprint(conn.getSiteUrl(), conn.getEmail(), conn.getApiTokenEncrypted(),
                Integer.parseInt(sprint.getJiraSprintId()));

        // Move issues to backlog
        issueRepository.findByProjectIdAndSprintId(sprint.getProjectId(), sprint.getId())
                .forEach(issue -> {
                    issue.setSprintId(null);
                    issueRepository.save(issue);
                });
        sprintRepository.delete(sprint);
        log.info("Deleted sprint '{}'", sprint.getSprintName());
    }

    // ---- helpers ----

    private JiraConnection getConnection(Integer projectId) {
        return connectionRepository.findByProjectId(projectId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Jira connection not found for project: " + projectId));
    }

    private void updateSprintFromDto(JiraSprint sprint, JiraSprintDto dto) {
        sprint.setSprintName(dto.getName());
        sprint.setSprintGoal(dto.getGoal());
        if (dto.getState() != null)
            sprint.setStatus(parseSprintStatus(dto.getState()));
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty())
            sprint.setStartDate(parseLocalDate(dto.getStartDate()));
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty())
            sprint.setEndDate(parseLocalDate(dto.getEndDate()));
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
            if (dateStr.contains("T"))
                return LocalDate.parse(dateStr.substring(0, 10));
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    private JiraSprintResponse mapToResponse(JiraSprint sprint) {
        var issues = issueRepository.findByProjectIdAndSprintId(sprint.getProjectId(), sprint.getId());
        long doneIssues = issues.stream().filter(i -> "Done".equalsIgnoreCase(i.getStatus())).count();

        return JiraSprintResponse.builder()
                .sprintId(sprint.getId()).projectId(sprint.getProjectId())
                .jiraSprintId(sprint.getJiraSprintId())
                .sprintName(sprint.getSprintName()).sprintGoal(sprint.getSprintGoal())
                .startDate(sprint.getStartDate()).endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .totalIssues((long) issues.size()).doneIssues(doneIssues)
                .build();
    }
}
