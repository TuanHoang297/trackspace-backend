package com.trackspace.github.service;

import com.trackspace.github.dto.ConnectionRequest;
import com.trackspace.github.dto.ConnectionStatusResponse;

/**
 * Service interface for managing GitHub repository connections
 */
public interface ConnectionService {

    /**
     * Connect a GitHub repository to a project
     * 
     * @param request Connection request with project ID, repo URL, and token
     * @return Connection status response
     */
    ConnectionStatusResponse connectRepository(ConnectionRequest request);

    /**
     * Get connection status for a project
     * 
     * @param projectId Project ID
     * @return Connection status response
     */
    ConnectionStatusResponse getConnectionStatus(Integer projectId);

    /**
     * Disconnect GitHub repository from a project
     * 
     * @param projectId Project ID
     */
    void disconnectRepository(Integer projectId);

    /**
     * Check if a project has an active GitHub connection
     * 
     * @param projectId Project ID
     * @return true if connected
     */
    boolean isConnected(Integer projectId);

    /**
     * Get the raw connection entity for a project
     *
     * @param projectId Project ID
     * @return Connection entity
     */
    com.trackspace.github.entity.Connection getConnection(Integer projectId);

    /**
     * Get ALL connections for a project (multi-repo)
     */
    java.util.List<ConnectionStatusResponse> getConnections(Integer projectId);
}
