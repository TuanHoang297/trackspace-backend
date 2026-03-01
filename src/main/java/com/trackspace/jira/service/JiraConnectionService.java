package com.trackspace.jira.service;

import com.trackspace.jira.dto.JiraConnectionRequest;
import com.trackspace.jira.dto.JiraConnectionResponse;

/**
 * Service interface for managing Jira connections
 */
public interface JiraConnectionService {

    /**
     * Connect a Jira project to a TrackSpace project
     */
    JiraConnectionResponse connect(JiraConnectionRequest request);

    /**
     * Get connection status for a project
     */
    JiraConnectionResponse getConnectionStatus(Integer projectId);

    /**
     * Disconnect Jira from a project
     */
    void disconnect(Integer projectId);

    /**
     * Check if a project has an active Jira connection
     */
    boolean isConnected(Integer projectId);
}
