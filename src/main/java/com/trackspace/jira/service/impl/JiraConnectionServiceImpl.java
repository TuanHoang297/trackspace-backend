package com.trackspace.jira.service.impl;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.JiraConnectionStatus;
import com.trackspace.jira.dto.JiraConnectionRequest;
import com.trackspace.jira.dto.JiraConnectionResponse;
import com.trackspace.jira.entity.JiraConnection;
import com.trackspace.jira.repository.JiraConnectionRepository;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.jira.repository.JiraSprintRepository;
import com.trackspace.jira.service.JiraApiClient;
import com.trackspace.jira.service.JiraConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of JiraConnectionService
 * Handles Jira project connection management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraConnectionServiceImpl implements JiraConnectionService {

    private final JiraConnectionRepository connectionRepository;
    private final JiraSprintRepository sprintRepository;
    private final JiraIssueRepository issueRepository;
    private final JiraApiClient jiraApiClient;

    @Override
    @Transactional
    public JiraConnectionResponse connect(JiraConnectionRequest request) {
        log.info("Connecting Jira for project {}", request.getProjectId());

        // Check if connection already exists for this project
        connectionRepository.findByProjectId(request.getProjectId()).ifPresent(existing -> {
            if (existing.getConnectionStatus() == JiraConnectionStatus.CONNECTED) {
                throw new BadRequestException(
                        "Dự án này đã được kết nối Jira. Hãy ngắt kết nối trước khi kết nối lại.");
            }
            // Clean up old DISCONNECTED/ERROR record
            issueRepository.deleteAll(issueRepository.findByProjectId(request.getProjectId()));
            sprintRepository.deleteAll(sprintRepository.findByProjectIdOrderByStartDateDesc(request.getProjectId()));
            connectionRepository.delete(existing);
            log.info("Cleaned up old {} connection for project {}", existing.getConnectionStatus(), request.getProjectId());
        });

        // Check if another project already uses this Jira project key
        connectionRepository.findByProjectKey(request.getProjectKey()).ifPresent(existing -> {
            if (existing.getConnectionStatus() == JiraConnectionStatus.CONNECTED) {
                throw new BadRequestException(
                        "Jira project key '" + request.getProjectKey()
                                + "' đã được kết nối với dự án khác (projectId="
                                + existing.getProjectId() + "). Mỗi Jira project chỉ kết nối được 1 dự án.");
            }
            // Clean up stale record from another project
            connectionRepository.delete(existing);
        });

        // Validate connection with Jira API (throws BadRequestException on failure)
        jiraApiClient.validateConnection(
                request.getSiteUrl(), request.getEmail(),
                request.getApiToken(), request.getProjectKey());

        // Create connection entity
        JiraConnection connection = new JiraConnection();
        connection.setProjectId(request.getProjectId());
        connection.setSiteUrl(request.getSiteUrl());
        connection.setEmail(request.getEmail());
        connection.setApiTokenEncrypted(request.getApiToken()); // TODO: Encrypt token
        connection.setProjectKey(request.getProjectKey());
        connection.setConnectionStatus(JiraConnectionStatus.CONNECTED);
        connection.setLastSyncAt(null);

        // Save connection
        JiraConnection saved = connectionRepository.save(connection);
        log.info("Successfully connected Jira for project {}", request.getProjectId());

        return buildConnectionResponse(saved);
    }

    @Override
    public JiraConnectionResponse getConnectionStatus(Integer projectId) {
        log.debug("Getting Jira connection status for project {}", projectId);

        JiraConnection connection = connectionRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + projectId));

        return buildConnectionResponse(connection);
    }

    @Override
    @Transactional
    public void disconnect(Integer projectId) {
        log.info("Disconnecting Jira for project {}", projectId);

        JiraConnection connection = connectionRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira connection not found for project: " + projectId));

        // Delete all related data first (issues → sprints → connection)
        long deletedIssues = issueRepository.countByProjectId(projectId);
        issueRepository.deleteAll(issueRepository.findByProjectId(projectId));

        long deletedSprints = sprintRepository.countByProjectId(projectId);
        sprintRepository.deleteAll(sprintRepository.findByProjectIdOrderByStartDateDesc(projectId));

        connectionRepository.delete(connection);

        log.info("Disconnected Jira for project {} — removed {} issues, {} sprints",
                projectId, deletedIssues, deletedSprints);
    }

    @Override
    public boolean isConnected(Integer projectId) {
        return connectionRepository.findByProjectId(projectId)
                .map(conn -> conn.getConnectionStatus() == JiraConnectionStatus.CONNECTED)
                .orElse(false);
    }

    private JiraConnectionResponse buildConnectionResponse(JiraConnection connection) {
        long totalSprints = sprintRepository.countByProjectId(connection.getProjectId());
        long totalIssues = issueRepository.countByProjectId(connection.getProjectId());

        return JiraConnectionResponse.builder()
                .connectionId(connection.getId())
                .projectId(connection.getProjectId())
                .siteUrl(connection.getSiteUrl())
                .email(connection.getEmail())
                .projectKey(connection.getProjectKey())
                .connectionStatus(connection.getConnectionStatus())
                .lastSyncAt(connection.getLastSyncAt())
                .totalSprints(totalSprints)
                .totalIssues(totalIssues)
                .build();
    }
}
