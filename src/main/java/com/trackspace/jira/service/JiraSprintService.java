package com.trackspace.jira.service;

import com.trackspace.jira.dto.JiraSprintResponse;
import com.trackspace.jira.dto.JiraSyncRequest;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing Jira sprints
 */
public interface JiraSprintService {

    /**
     * Get all sprints for a project
     */
    List<JiraSprintResponse> getSprints(Integer projectId);

    /**
     * Sync sprints from Jira
     */
    Map<String, Object> syncSprints(JiraSyncRequest request);
}
