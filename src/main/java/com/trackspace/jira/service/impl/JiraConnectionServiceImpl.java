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

import java.time.Instant;

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

        // Check if connection already exists
        if (connectionRepository.existsByProjectId(request.getProjectId())) {
            throw new BadRequestException("Jira connection already exists for this project");
        }

        // Validate connection with Jira API
        boolean isValid = jiraApiClient.validateConnection(
                request.getSiteUrl(), request.getEmail(),
                request.getApiToken(), request.getProjectKey());

        if (!isValid) {
            throw new BadRequestException("Invalid Jira credentials or no access to project");
        }

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

        connection.setConnectionStatus(JiraConnectionStatus.DISCONNECTED);
        connection.setApiTokenEncrypted(""); // Clear token
        connection.setUpdatedAt(Instant.now());

        connectionRepository.save(connection);
        log.info("Successfully disconnected Jira for project {}", projectId);
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
