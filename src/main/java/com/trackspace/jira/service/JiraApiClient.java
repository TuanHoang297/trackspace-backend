package com.trackspace.jira.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client for Jira Cloud REST API v3
 * Handles all external API calls to Jira
 * Uses RestTemplate with Basic Auth (email + API token)
 */
@Service
@Slf4j
public class JiraApiClient {

    private final RestTemplate restTemplate;

    public JiraApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Validate connection to Jira Cloud
     */
    public boolean validateConnection(String siteUrl, String email, String apiToken, String projectKey) {
        log.info("Validating Jira connection to {} for project key {}", siteUrl, projectKey);

        try {
            String url = buildBaseUrl(siteUrl) + "/rest/api/3/project/" + projectKey;
            HttpHeaders headers = createHeaders(email, apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<JiraProjectDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JiraProjectDto.class);

            log.info("Successfully validated Jira connection for project key {}", projectKey);
            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException ex) {
            log.error("Failed to validate Jira connection: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Fetch sprints from Jira board
     * Uses Jira Agile REST API to get sprints for a board
     */
    public List<JiraSprintDto> fetchSprints(String siteUrl, String email, String apiToken, String projectKey) {
        log.info("Fetching sprints for project key {} from {}", projectKey, siteUrl);

        try {
            // First, find the board ID for this project
            Integer boardId = findBoardId(siteUrl, email, apiToken, projectKey);
            if (boardId == null) {
                log.warn("No board found for project key {}", projectKey);
                return Collections.emptyList();
            }

            String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/board/" + boardId + "/sprint?maxResults=100";
            HttpHeaders headers = createHeaders(email, apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<JiraSprintListDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JiraSprintListDto.class);

            if (response.getBody() != null && response.getBody().getValues() != null) {
                // Filter: only keep sprints that originate from this board
                List<JiraSprintDto> allSprints = response.getBody().getValues();
                List<JiraSprintDto> filtered = allSprints.stream()
                        .filter(s -> s.getOriginBoardId() == null || s.getOriginBoardId().equals(boardId))
                        .collect(java.util.stream.Collectors.toList());
                log.info("Fetched {} sprints for project {} (filtered from {})",
                        filtered.size(), projectKey, allSprints.size());
                return filtered;
            }

            return Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            String msg = handleJiraError(ex);
            log.error("Jira API error fetching sprints: {}", msg);
            throw new com.trackspace.common.BadRequestException(msg);
        } catch (RestClientException ex) {
            log.error("Error fetching sprints: {}", ex.getMessage());
            throw new com.trackspace.common.BadRequestException("Cannot connect to Jira API: " + ex.getMessage());
        }
    }

    /**
     * Fetch issues for a project from Jira using JQL
     */
    public List<JiraIssueDto> fetchIssues(String siteUrl, String email, String apiToken, String projectKey) {
        log.info("Fetching issues for project key {} from {}", projectKey, siteUrl);

        try {
            String jql = "project=" + projectKey + " ORDER BY updated DESC";
            String url = buildBaseUrl(siteUrl) + "/rest/api/3/search/jql?jql=" + jql
                    + "&maxResults=100&fields=summary,description,status,priority,issuetype,assignee,sprint,duedate";
            HttpHeaders headers = createHeaders(email, apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<JiraSearchResultDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JiraSearchResultDto.class);

            if (response.getBody() != null && response.getBody().getIssues() != null) {
                log.info("Fetched {} issues for project {}", response.getBody().getIssues().size(), projectKey);
                return response.getBody().getIssues();
            }

            return Collections.emptyList();

        } catch (HttpClientErrorException ex) {
            String msg = handleJiraError(ex);
            log.error("Jira API error fetching issues: {}", msg);
            throw new com.trackspace.common.BadRequestException(msg);
        } catch (RestClientException ex) {
            log.error("Error fetching issues: {}", ex.getMessage());
            throw new com.trackspace.common.BadRequestException("Cannot connect to Jira API: " + ex.getMessage());
        }
    }

    /**
     * Fetch issue keys for a specific sprint using Jira Agile REST API
     * GET /rest/agile/1.0/sprint/{sprintId}/issue
     */
    public List<String> fetchIssueKeysForSprint(String siteUrl, String email, String apiToken, int jiraSprintId) {
        try {
            String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/sprint/" + jiraSprintId
                    + "/issue?maxResults=200&fields=key";
            HttpHeaders headers = createHeaders(email, apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<JiraSearchResultDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JiraSearchResultDto.class);

            if (response.getBody() != null && response.getBody().getIssues() != null) {
                return response.getBody().getIssues().stream()
                        .map(JiraIssueDto::getKey)
                        .toList();
            }
            return List.of();
        } catch (Exception ex) {
            log.warn("Failed to fetch issues for sprint {}: {}", jiraSprintId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Create an issue in Jira
     */
    public JiraIssueDto createIssue(String siteUrl, String email, String apiToken,
            String projectKey, String issueType, String summary,
            String description, String priority) {
        log.info("Creating issue in Jira project {}: {}", projectKey, summary);

        try {
            String url = buildBaseUrl(siteUrl) + "/rest/api/3/issue";
            HttpHeaders headers = createHeaders(email, apiToken);

            Map<String, Object> fields = new HashMap<>();
            Map<String, Object> project = Map.of("key", projectKey);
            Map<String, Object> type = Map.of("name", issueType);

            fields.put("project", project);
            fields.put("issuetype", type);
            fields.put("summary", summary);

            if (description != null && !description.isEmpty()) {
                // Jira API v3 uses ADF (Atlassian Document Format) for description
                Map<String, Object> descContent = Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", List.of(
                                Map.of("type", "paragraph",
                                        "content", List.of(
                                                Map.of("type", "text", "text", description)))));
                fields.put("description", descContent);
            }

            if (priority != null && !priority.isEmpty()) {
                fields.put("priority", Map.of("name", priority));
            }

            Map<String, Object> body = Map.of("fields", fields);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JiraIssueDto> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JiraIssueDto.class);

            log.info("Successfully created Jira issue: {}",
                    response.getBody() != null ? response.getBody().getKey() : "unknown");
            return response.getBody();

        } catch (HttpClientErrorException ex) {
            String msg = handleJiraError(ex);
            log.error("Jira API error creating issue: {}", msg);
            throw new com.trackspace.common.BadRequestException(msg);
        } catch (RestClientException ex) {
            log.error("Error creating Jira issue: {}", ex.getMessage());
            throw new com.trackspace.common.BadRequestException("Cannot connect to Jira API: " + ex.getMessage());
        }
    }

    /**
     * Update issue status via transition in Jira
     */
    public void updateIssueStatus(String siteUrl, String email, String apiToken,
            String issueKey, String targetStatus) {
        log.info("Updating issue {} status to {}", issueKey, targetStatus);

        try {
            // First, get available transitions
            String transitionsUrl = buildBaseUrl(siteUrl) + "/rest/api/3/issue/" + issueKey + "/transitions";
            HttpHeaders headers = createHeaders(email, apiToken);
            HttpEntity<?> getEntity = new HttpEntity<>(headers);

            ResponseEntity<JiraTransitionsDto> transResponse = restTemplate.exchange(
                    transitionsUrl, HttpMethod.GET, getEntity, JiraTransitionsDto.class);

            if (transResponse.getBody() == null || transResponse.getBody().getTransitions() == null) {
                throw new com.trackspace.common.BadRequestException("No transitions available for issue " + issueKey);
            }

            // Find matching transition
            String transitionId = transResponse.getBody().getTransitions().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(targetStatus)
                            || (t.getTo() != null && t.getTo().getName().equalsIgnoreCase(targetStatus)))
                    .map(JiraTransitionDto::getId)
                    .findFirst()
                    .orElseThrow(() -> new com.trackspace.common.BadRequestException(
                            "No transition found to status: " + targetStatus));

            // Execute transition
            Map<String, Object> body = Map.of("transition", Map.of("id", transitionId));
            HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(body, headers);

            restTemplate.exchange(transitionsUrl, HttpMethod.POST, postEntity, Void.class);

            log.info("Successfully transitioned issue {} to {}", issueKey, targetStatus);

        } catch (HttpClientErrorException ex) {
            String msg = handleJiraError(ex);
            log.error("Jira API error updating issue status: {}", msg);
            throw new com.trackspace.common.BadRequestException(msg);
        } catch (RestClientException ex) {
            log.error("Error updating Jira issue status: {}", ex.getMessage());
            throw new com.trackspace.common.BadRequestException("Cannot connect to Jira API: " + ex.getMessage());
        }
    }

    /**
     * Move an issue to a specific sprint using Jira Agile REST API
     * POST /rest/agile/1.0/sprint/{sprintId}/issue
     */
    public void moveIssueToSprint(String siteUrl, String email, String apiToken,
            String jiraSprintId, String issueKey) {
        log.info("Moving issue {} to sprint {}", issueKey, jiraSprintId);

        try {
            String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/sprint/" + jiraSprintId + "/issue";
            HttpHeaders headers = createHeaders(email, apiToken);

            Map<String, Object> body = Map.of("issues", List.of(issueKey));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("Successfully moved issue {} to sprint {}", issueKey, jiraSprintId);

        } catch (HttpClientErrorException ex) {
            String msg = handleJiraError(ex);
            log.error("Jira API error moving issue to sprint: {}", msg);
            throw new com.trackspace.common.BadRequestException(msg);
        } catch (RestClientException ex) {
            log.error("Error moving issue to sprint: {}", ex.getMessage());
            throw new com.trackspace.common.BadRequestException("Cannot move issue to sprint: " + ex.getMessage());
        }
    }

    /**
     * Update issue fields in Jira (summary, description, priority, issue type)
     * PUT /rest/api/3/issue/{issueKey}
     */
    @SuppressWarnings("unchecked")
    public void updateIssue(String siteUrl, String email, String apiToken,
            String issueKey, String summary, String description, String priority, String issueType, String dueDate) {
        String url = buildBaseUrl(siteUrl) + "/rest/api/3/issue/" + issueKey;
        HttpHeaders headers = createHeaders(email, apiToken);

        Map<String, Object> fields = new HashMap<>();
        if (summary != null)
            fields.put("summary", summary);
        if (priority != null)
            fields.put("priority", Map.of("name", priority));
        if (issueType != null)
            fields.put("issuetype", Map.of("name", issueType));
        if (dueDate != null)
            fields.put("duedate", dueDate);
        if (description != null) {
            // Convert plain text to ADF format
            fields.put("description", Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", List.of(Map.of(
                            "type", "paragraph",
                            "content", List.of(Map.of("type", "text", "text", description))))));
        }

        Map<String, Object> body = Map.of("fields", fields);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Updated issue {} on Jira", issueKey);
        } catch (HttpClientErrorException ex) {
            log.error("Failed to update issue {}: {}", issueKey, ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Delete an issue from Jira
     * DELETE /rest/api/3/issue/{issueKey}
     */
    public void deleteIssue(String siteUrl, String email, String apiToken, String issueKey) {
        String url = buildBaseUrl(siteUrl) + "/rest/api/3/issue/" + issueKey;
        HttpHeaders headers = createHeaders(email, apiToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Deleted issue {} from Jira", issueKey);
        } catch (HttpClientErrorException ex) {
            log.error("Failed to delete issue {}: {}", issueKey, ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Assign an issue to a user on Jira
     * PUT /rest/api/3/issue/{issueKey}/assignee
     */
    public void assignIssueOnJira(String siteUrl, String email, String apiToken,
            String issueKey, String accountId) {
        String url = buildBaseUrl(siteUrl) + "/rest/api/3/issue/" + issueKey + "/assignee";
        HttpHeaders headers = createHeaders(email, apiToken);

        Map<String, String> body = new HashMap<>();
        body.put("accountId", accountId);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Assigned issue {} to accountId {} on Jira", issueKey, accountId);
        } catch (HttpClientErrorException ex) {
            log.error("Failed to assign issue {}: {}", issueKey, ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Get assignable users for a Jira project
     * GET /rest/api/3/user/assignable/search?project={projectKey}
     */
    @SuppressWarnings("unchecked")
    public List<JiraUserDto> getAssignableUsers(String siteUrl, String email, String apiToken,
            String projectKey) {
        String url = buildBaseUrl(siteUrl) + "/rest/api/3/user/assignable/search?project=" + projectKey;
        HttpHeaders headers = createHeaders(email, apiToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, entity, JiraUserDto[].class);
            JiraUserDto[] users = response.getBody();
            log.info("Found {} assignable users for project {}", users != null ? users.length : 0, projectKey);
            return users != null ? List.of(users) : List.of();
        } catch (HttpClientErrorException ex) {
            log.error("Failed to get assignable users for {}: {}", projectKey, ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Create a sprint in Jira
     * POST /rest/agile/1.0/sprint
     */
    @SuppressWarnings("unchecked")
    public JiraSprintDto createSprint(String siteUrl, String email, String apiToken,
            String projectKey, String name, String startDate, String endDate, String goal) {
        Integer boardId = findBoardId(siteUrl, email, apiToken, projectKey);
        if (boardId == null) {
            throw new RuntimeException("Cannot find Jira board for project " + projectKey);
        }

        String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/sprint";
        HttpHeaders headers = createHeaders(email, apiToken);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("originBoardId", boardId);
        if (startDate != null)
            body.put("startDate", startDate);
        if (endDate != null)
            body.put("endDate", endDate);
        if (goal != null)
            body.put("goal", goal);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JiraSprintDto> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JiraSprintDto.class);
            log.info("Created sprint '{}' on Jira", name);
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.error("Failed to create sprint: {}", ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Update a sprint in Jira
     * PUT /rest/agile/1.0/sprint/{sprintId}
     */
    @SuppressWarnings("unchecked")
    public void updateSprint(String siteUrl, String email, String apiToken,
            int jiraSprintId, String name, String startDate, String endDate, String goal, String state) {
        String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/sprint/" + jiraSprintId;
        HttpHeaders headers = createHeaders(email, apiToken);

        Map<String, Object> body = new HashMap<>();
        if (name != null)
            body.put("name", name);
        if (startDate != null)
            body.put("startDate", startDate);
        if (endDate != null)
            body.put("endDate", endDate);
        if (goal != null)
            body.put("goal", goal);
        if (state != null)
            body.put("state", state);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Updated sprint {} on Jira", jiraSprintId);
        } catch (HttpClientErrorException ex) {
            log.error("Failed to update sprint {}: {}", jiraSprintId, ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Delete a sprint from Jira
     * DELETE /rest/agile/1.0/sprint/{sprintId}
     */
    public void deleteSprint(String siteUrl, String email, String apiToken, int jiraSprintId) {
        String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/sprint/" + jiraSprintId;
        HttpHeaders headers = createHeaders(email, apiToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Deleted sprint {} from Jira", jiraSprintId);
        } catch (HttpClientErrorException ex) {
            log.error("Failed to delete sprint {}: {}", jiraSprintId, ex.getResponseBodyAsString());
            throw new RuntimeException(handleJiraError(ex));
        }
    }

    /**
     * Find board ID for a project
     */
    private Integer findBoardId(String siteUrl, String email, String apiToken, String projectKey) {
        try {
            String url = buildBaseUrl(siteUrl) + "/rest/agile/1.0/board?projectKeyOrId=" + projectKey;
            HttpHeaders headers = createHeaders(email, apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<JiraBoardListDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JiraBoardListDto.class);

            if (response.getBody() != null && response.getBody().getValues() != null
                    && !response.getBody().getValues().isEmpty()) {
                return response.getBody().getValues().get(0).getId();
            }

            return null;
        } catch (RestClientException ex) {
            log.warn("Could not find board for project {}: {}", projectKey, ex.getMessage());
            return null;
        }
    }

    /**
     * Build base URL from site URL
     * Handles both "https://site.atlassian.net" and "site.atlassian.net"
     */
    private String buildBaseUrl(String siteUrl) {
        String url = siteUrl.trim();
        if (!url.startsWith("https://")) {
            url = "https://" + url;
        }
        // Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Create HTTP headers with Basic Auth for Jira API
     */
    private HttpHeaders createHeaders(String email, String apiToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(email, apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * Handle Jira API error responses
     */
    private String handleJiraError(HttpClientErrorException ex) {
        return switch (ex.getStatusCode().value()) {
            case 401 -> "Jira credentials are invalid or expired";
            case 403 -> "No permission to access this Jira project";
            case 404 -> "Jira project not found";
            default -> "Jira API error: " + ex.getMessage();
        };
    }

    // ============== DTOs for Jira API responses ==============

    @Data
    public static class JiraProjectDto {
        private String id;
        private String key;
        private String name;
    }

    @Data
    public static class JiraBoardListDto {
        private List<JiraBoardDto> values;
    }

    @Data
    public static class JiraBoardDto {
        private Integer id;
        private String name;
        private String type;
    }

    @Data
    public static class JiraSprintListDto {
        private List<JiraSprintDto> values;
    }

    @Data
    public static class JiraSprintDto {
        private Integer id;
        private String name;
        private String state; // future, active, closed
        private String goal;
        private String startDate;
        private String endDate;
        private Integer originBoardId;
    }

    @Data
    public static class JiraSearchResultDto {
        private Integer total;
        private List<JiraIssueDto> issues;
    }

    @Data
    public static class JiraIssueDto {
        private String id;
        private String key;
        private String self;
        private JiraIssueFieldsDto fields;
    }

    @Data
    public static class JiraIssueFieldsDto {
        private String summary;
        private Object description; // ADF format in v3
        private JiraStatusDto status;
        private JiraPriorityDto priority;
        private JiraIssueTypeDto issuetype;
        private JiraUserDto assignee;
        private String duedate;

        @JsonProperty("sprint")
        private JiraSprintDto sprint;
    }

    @Data
    public static class JiraStatusDto {
        private String name;
        private String id;
    }

    @Data
    public static class JiraPriorityDto {
        private String name;
        private String id;
    }

    @Data
    public static class JiraIssueTypeDto {
        private String name;
        private String id;
    }

    @Data
    public static class JiraUserDto {
        @JsonProperty("accountId")
        private String accountId;

        @JsonProperty("displayName")
        private String displayName;

        @JsonProperty("emailAddress")
        private String emailAddress;
    }

    @Data
    public static class JiraTransitionsDto {
        private List<JiraTransitionDto> transitions;
    }

    @Data
    public static class JiraTransitionDto {
        private String id;
        private String name;
        private JiraStatusDto to;
    }
}
