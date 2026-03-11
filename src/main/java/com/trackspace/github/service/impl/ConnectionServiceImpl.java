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
import java.util.List;

/**
 * Implementation of ConnectionService
 * Handles GitHub repository connection management
 * Supports multiple repos per project (FE + BE)
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

        // Check if THIS EXACT REPO already has a connection — reconnect if disconnected
        List<Connection> existing = connectionRepository.findByProjectId(request.getProjectId());
        Connection existingConn = existing.stream()
                .filter(c -> request.getRepositoryUrl().equals(c.getRepositoryUrl()))
                .findFirst().orElse(null);
        if (existingConn != null) {
            if (existingConn.getStatus() == ConnectionStatus.CONNECTED) {
                // Already connected — just return current status
                return buildConnectionStatusResponse(existingConn);
            }
            // Reconnect: update token and set status back
            existingConn.setAccessTokenEncrypted(request.getAccessToken());
            existingConn.setStatus(ConnectionStatus.CONNECTED);
            existingConn.setUpdatedAt(Instant.now());
            Connection saved = connectionRepository.save(existingConn);
            log.info("Reconnected existing GitHub repository for project {}", request.getProjectId());
            return buildConnectionStatusResponse(saved);
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

        Connection connection = new Connection();
        connection.setProjectId(request.getProjectId());
        connection.setRepositoryUrl(request.getRepositoryUrl());
        connection.setRepoLabel(request.getRepoLabel());
        connection.setBranchName(branchName);
        connection.setAccessTokenEncrypted(request.getAccessToken());
        connection.setStatus(ConnectionStatus.CONNECTED);
        connection.setLastSyncAt(null);

        // Save connection
        Connection saved = connectionRepository.save(connection);
        log.info("Successfully connected GitHub repository {} for project {}",
                request.getRepositoryUrl(), request.getProjectId());

        return buildConnectionStatusResponse(saved);
    }

    @Override
    public ConnectionStatusResponse getConnectionStatus(Integer projectId) {
        log.debug("Getting connection status for project {}", projectId);

        Connection connection = connectionRepository.findFirstByProjectIdOrderByIdAsc(projectId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("GitHub connection not found for project: " + projectId));

        return buildConnectionStatusResponse(connection);
    }

    @Override
    @Transactional
    public void disconnectRepository(Integer projectId) {
        log.info("Disconnecting ALL GitHub repositories for project {}", projectId);

        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        if (connections.isEmpty()) {
            throw new ResourceNotFoundException("GitHub connection not found for project: " + projectId);
        }

        // Delete all related commits first, then connections
        long deletedCommits = 0;
        for (Connection connection : connections) {
            long count = commitRepository.countByConnectionId(connection.getId());
            commitRepository.deleteByConnectionId(connection.getId());
            deletedCommits += count;
        }
        connectionRepository.deleteAll(connections);

        log.info("Disconnected {} GitHub repositories for project {} — removed {} commits",
                connections.size(), projectId, deletedCommits);
    }

    @Override
    @Transactional
    public void disconnectSingleRepository(Integer connectionId) {
        log.info("Disconnecting single GitHub connection {}", connectionId);

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found: " + connectionId));

        long count = commitRepository.countByConnectionId(connectionId);
        commitRepository.deleteByConnectionId(connectionId);
        connectionRepository.delete(connection);

        log.info("Disconnected GitHub connection {} (repo: {}) — removed {} commits",
                connectionId, connection.getRepositoryUrl(), count);
    }

    @Override
    public boolean isConnected(Integer projectId) {
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        return connections.stream().anyMatch(conn -> conn.getStatus() == ConnectionStatus.CONNECTED);
    }

    @Override
    public Connection getConnection(Integer projectId) {
        return connectionRepository.findFirstByProjectIdOrderByIdAsc(projectId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("GitHub connection not found for project: " + projectId));
    }

    @Override
    public Connection getConnectionById(Integer connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("GitHub connection not found: " + connectionId));
    }

    @Override
    public List<ConnectionStatusResponse> getConnections(Integer projectId) {
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        return connections.stream()
                .map(this::buildConnectionStatusResponse)
                .collect(java.util.stream.Collectors.toList());
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
        // Count commits scoped to this specific connection (not all project commits)
        Long totalCommits = commitRepository.countByConnectionId(connection.getId());

        return ConnectionStatusResponse.builder()
                .connectionId(connection.getId())
                .projectId(connection.getProjectId())
                .repositoryUrl(connection.getRepositoryUrl())
                .branchName(connection.getBranchName())
                .connectionStatus(connection.getStatus())
                .lastSyncAt(connection.getLastSyncAt())
                .totalCommits(totalCommits)
                .repoLabel(connection.getRepoLabel())
                .build();
    }
}
