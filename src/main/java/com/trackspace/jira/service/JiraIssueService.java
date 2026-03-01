package com.trackspace.jira.service;

import com.trackspace.jira.dto.JiraIssueRequest;
import com.trackspace.jira.dto.JiraIssueResponse;
import com.trackspace.jira.dto.JiraSyncRequest;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing Jira issues
 */
public interface JiraIssueService {

    /**
     * Get issues for a project with optional filters
     */
    List<JiraIssueResponse> getIssues(Integer projectId, Integer sprintId, String status, Integer assigneeId);

    /**
     * Sync issues from Jira
     */
    Map<String, Object> syncIssues(JiraSyncRequest request);

    /**
     * Create a new issue and sync to Jira
     */
    JiraIssueResponse createIssue(JiraIssueRequest request);

    /**
     * Update issue status (syncs to Jira)
     */
    JiraIssueResponse updateIssueStatus(Integer issueId, String newStatus);

    /**
     * Assign issue to a user
     */
    JiraIssueResponse assignIssue(Integer issueId, Integer assigneeId);

    /**
     * Assign issue to a Jira user using their accountId (syncs to Jira)
     */
    JiraIssueResponse assignIssueOnJira(Integer issueId, String jiraAccountId, String displayName);

    /**
     * Get assignable users from Jira project
     */
    List<Map<String, String>> getAssignableUsers(Integer projectId);

    /**
     * Update issue fields (summary, description, priority, issueType) and sync to
     * Jira
     */
    JiraIssueResponse updateIssue(Integer issueId, JiraIssueRequest request);

    /**
     * Delete issue from both TrackSpace and Jira
     */
    void deleteIssue(Integer issueId);
}
