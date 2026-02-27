package com.trackspace.github.service.impl;

import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.github.ConnectionStatus;
import com.trackspace.github.dto.ConnectionRequest;
import com.trackspace.github.dto.ConnectionStatusResponse;
import com.trackspace.github.entity.Connection;
import com.trackspace.github.repository.CommitRepository;
import com.trackspace.github.repository.ConnectionRepository;
import com.trackspace.github.service.ConnectionService;
import com.trackspace.github.service.GitHubApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of ConnectionService
 * Handles GitHub repository connection management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final CommitRepository commitRepository;
    private final GitHubApiClient gitHubApiClient;

    @Override
    @Transactional
    public ConnectionStatusResponse connectRepository(ConnectionRequest request) {
        log.info("Connecting GitHub repository for project {}", request.getProjectId());

        // Check if connection already exists
        if (connectionRepository.existsByProjectId(request.getProjectId())) {
            throw new IllegalStateException("GitHub connection already exists for this project");
        }

        // Parse owner and repo from URL
        String[] ownerRepo = parseRepositoryUrl(request.getRepositoryUrl());
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];

        // Validate connection with GitHub API
        boolean isValid = gitHubApiClient.validateConnection(owner, repo, request.getAccessToken());

        if (!isValid) {
            throw new IllegalArgumentException("Invalid GitHub token or no access to repository");
        }

        // Get repository info to get default branch if not provided
        String branchName = request.getBranchName();
        if (branchName == null || branchName.isEmpty()) {
            GitHubApiClient.GitHubRepoDto repoInfo = gitHubApiClient
                    .getRepository(owner, repo, request.getAccessToken());
            branchName = repoInfo != null ? repoInfo.getDefaultBranch() : "main";
        }

        // Create connection entity
        Connection connection = new Connection();
        connection.setProjectId(request.getProjectId());
        connection.setRepositoryUrl(request.getRepositoryUrl());
        connection.setBranchName(branchName);
        connection.setAccessTokenEncrypted(request.getAccessToken()); // TODO: Encrypt token
        connection.setStatus(ConnectionStatus.CONNECTED);
        connection.setLastSyncAt(null);

        // Save connection
        Connection saved = connectionRepository.save(connection);
        log.info("Successfully connected GitHub repository for project {}", request.getProjectId());

        return buildConnectionStatusResponse(saved);
    }

    @Override
    public ConnectionStatusResponse getConnectionStatus(Integer projectId) {
        log.debug("Getting connection status for project {}", projectId);

        Connection connection = connectionRepository.findByProjectId(projectId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("GitHub connection not found for project: " + projectId));

        return buildConnectionStatusResponse(connection);
    }

    @Override
    @Transactional
    public void disconnectRepository(Integer projectId) {
        log.info("Disconnecting GitHub repository for project {}", projectId);

        Connection connection = connectionRepository.findByProjectId(projectId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("GitHub connection not found for project: " + projectId));

        // Update status to disconnected
        connection.setStatus(ConnectionStatus.DISCONNECTED);
        connection.setAccessTokenEncrypted(""); // Clear token on disconnect
        connection.setUpdatedAt(Instant.now());

        connectionRepository.save(connection);
        log.info("Successfully disconnected GitHub repository for project {}", projectId);
    }

    @Override
    public boolean isConnected(Integer projectId) {
        return connectionRepository.findByProjectId(projectId)
                .map(conn -> conn.getStatus() == ConnectionStatus.CONNECTED)
                .orElse(false);
    }

    /**
     * Parse GitHub repository URL to extract owner and repo name
     * Expected format: https://github.com/owner/repo
     * 
     * @param url Repository URL
     * @return Array [owner, repo]
     */
    private String[] parseRepositoryUrl(String url) {
        try {
            // Remove trailing .git if present
            String cleanUrl = url.replace(".git", "");

            // Remove https://github.com/ prefix
            String path = cleanUrl.replace("https://github.com/", "");

            // Split by /
            String[] parts = path.split("/");

            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid GitHub repository URL format");
            }

            return new String[] { parts[0], parts[1] };
        } catch (Exception e) {
            log.error("Failed to parse repository URL: {}", url, e);
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + url);
        }
    }

    /**
     * Build ConnectionStatusResponse from Connection entity
     */
    private ConnectionStatusResponse buildConnectionStatusResponse(Connection connection) {
        // Count total commits for this project
        Long totalCommits = commitRepository.findByProjectId(connection.getProjectId())
                .stream()
                .count();

        return ConnectionStatusResponse.builder()
                .connectionId(connection.getId())
                .projectId(connection.getProjectId())
                .repositoryUrl(connection.getRepositoryUrl())
                .branchName(connection.getBranchName())
                .connectionStatus(connection.getStatus())
                .lastSyncAt(connection.getLastSyncAt())
                .totalCommits(totalCommits)
                .build();
    }
}
