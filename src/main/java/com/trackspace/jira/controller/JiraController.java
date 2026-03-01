package com.trackspace.jira.controller;

import com.trackspace.common.ApiResponse;
import com.trackspace.jira.dto.*;
import com.trackspace.jira.service.JiraConnectionService;
import com.trackspace.jira.service.JiraIssueService;
import com.trackspace.jira.service.JiraSprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Jira Integration
 *
 * Provides endpoints for:
 * - Connecting/disconnecting a Jira project
 * - Syncing sprints and issues from Jira
 * - CRUD operations on issues
 * - Sprint board view
 */
@RestController
@RequestMapping("/api/v1/jira")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jira Integration", description = "Endpoints for connecting Jira projects, syncing sprints/issues, and managing tasks")
public class JiraController {

    private final JiraConnectionService connectionService;
    private final JiraSprintService sprintService;
    private final JiraIssueService issueService;

    // ==================== CONNECTION ENDPOINTS ====================

    /**
     * POST /api/v1/jira/connect
     * Connect a Jira project to a TrackSpace project
     */
    @Operation(summary = "Connect Jira project", description = """
            Connects a Jira Cloud project to a TrackSpace project.
            - Validates the provided credentials with the Jira API.
            - Only one Jira project can be connected per TrackSpace project.
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Jira project connected successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Jira project connected successfully",
                      "data": {
                        "connectionId": 1,
                        "projectId": 42,
                        "siteUrl": "https://myteam.atlassian.net",
                        "email": "user@fpt.edu.vn",
                        "projectKey": "TS",
                        "connectionStatus": "CONNECTED",
                        "lastSyncAt": null,
                        "totalSprints": 0,
                        "totalIssues": 0
                      }
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials or already connected")
    })
    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<JiraConnectionResponse>> connect(
            @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Jira connection details", required = true, content = @Content(schema = @Schema(implementation = JiraConnectionRequest.class), examples = @ExampleObject(value = """
                    {
                      "projectId": 42,
                      "siteUrl": "https://myteam.atlassian.net",
                      "email": "user@fpt.edu.vn",
                      "apiToken": "ATATT3xFfGF0...",
                      "projectKey": "TS"
                    }
                    """))) JiraConnectionRequest request) {

        log.info("POST /api/v1/jira/connect - projectId={}", request.getProjectId());
        JiraConnectionResponse response = connectionService.connect(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Jira project connected successfully", response));
    }

    /**
     * GET /api/v1/jira/status/{projectId}
     */
    @Operation(summary = "Get Jira connection status")
    @GetMapping("/status/{projectId}")
    public ResponseEntity<ApiResponse<JiraConnectionResponse>> getConnectionStatus(
            @Parameter(description = "Project ID", required = true, example = "42") @PathVariable Integer projectId) {

        log.debug("GET /api/v1/jira/status/{}", projectId);
        JiraConnectionResponse response = connectionService.getConnectionStatus(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/jira/disconnect/{projectId}
     */
    @Operation(summary = "Disconnect Jira project", description = "Disconnects Jira from a project. Existing data is preserved.")
    @DeleteMapping("/disconnect/{projectId}")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @Parameter(description = "Project ID", required = true, example = "42") @PathVariable Integer projectId) {

        log.info("DELETE /api/v1/jira/disconnect/{}", projectId);
        connectionService.disconnect(projectId);
        return ResponseEntity.ok(ApiResponse.success("Jira project disconnected successfully", null));
    }

    // ==================== SYNC ENDPOINTS ====================

    /**
     * POST /api/v1/jira/sync
     * Sync sprints and issues from Jira
     */
    @Operation(summary = "Sync data from Jira", description = """
            Fetches latest sprints and issues from Jira and saves them to the database.
            - Existing data is updated if changed.
            - New data is created automatically.
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sync completed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Sync completed",
                      "data": {
                        "sprints": { "sprintsSynced": 2, "sprintsUpdated": 1 },
                        "issues": { "issuesSynced": 15, "issuesUpdated": 5 }
                      }
                    }
                    """)))
    })
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sync(
            @Valid @RequestBody JiraSyncRequest request) {

        log.info("POST /api/v1/jira/sync - projectId={}", request.getProjectId());

        Map<String, Object> sprintResult = sprintService.syncSprints(request);
        Map<String, Object> issueResult = issueService.syncIssues(request);

        Map<String, Object> result = Map.of(
                "sprints", sprintResult,
                "issues", issueResult);

        return ResponseEntity.ok(ApiResponse.success("Sync completed", result));
    }

    // ==================== SPRINT ENDPOINTS ====================

    /**
     * GET /api/v1/jira/sprints/{projectId}
     */
    @Operation(summary = "Get sprints for a project", description = "Returns all sprints with progress info (total issues, done issues)")
    @GetMapping("/sprints/{projectId}")
    public ResponseEntity<ApiResponse<List<JiraSprintResponse>>> getSprints(
            @Parameter(description = "Project ID", required = true, example = "42") @PathVariable Integer projectId) {

        log.debug("GET /api/v1/jira/sprints/{}", projectId);
        List<JiraSprintResponse> sprints = sprintService.getSprints(projectId);
        return ResponseEntity.ok(ApiResponse.success(sprints));
    }

    // ==================== ISSUE ENDPOINTS ====================

    /**
     * GET /api/v1/jira/issues/{projectId}
     */
    @Operation(summary = "Get issues for a project", description = """
            Returns issues with optional filters:
            - `sprintId`: filter by sprint
            - `status`: filter by status (To Do, In Progress, Done)
            - `assigneeId`: filter by assigned user
            """)
    @GetMapping("/issues/{projectId}")
    public ResponseEntity<ApiResponse<List<JiraIssueResponse>>> getIssues(
            @Parameter(description = "Project ID", required = true, example = "42") @PathVariable Integer projectId,

            @Parameter(description = "Filter by sprint ID") @RequestParam(required = false) Integer sprintId,

            @Parameter(description = "Filter by status (e.g. 'To Do', 'In Progress', 'Done')") @RequestParam(required = false) String status,

            @Parameter(description = "Filter by assignee user ID") @RequestParam(required = false) Integer assigneeId) {

        log.debug("GET /api/v1/jira/issues/{} - sprint={}, status={}, assignee={}",
                projectId, sprintId, status, assigneeId);
        List<JiraIssueResponse> issues = issueService.getIssues(projectId, sprintId, status, assigneeId);
        return ResponseEntity.ok(ApiResponse.success(issues));
    }

    /**
     * POST /api/v1/jira/issues
     * Create a new issue (syncs to Jira)
     */
    @Operation(summary = "Create issue", description = "Creates an issue in both TrackSpace and Jira")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Issue created successfully")
    })
    @PostMapping("/issues")
    public ResponseEntity<ApiResponse<JiraIssueResponse>> createIssue(
            @Valid @RequestBody JiraIssueRequest request) {

        log.info("POST /api/v1/jira/issues - projectId={}, summary={}", request.getProjectId(), request.getSummary());
        JiraIssueResponse response = issueService.createIssue(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Issue created successfully", response));
    }

    /**
     * PUT /api/v1/jira/issues/{issueId}/status
     * Update issue status (syncs to Jira via transitions)
     */
    @Operation(summary = "Update issue status", description = "Updates issue status in both TrackSpace and Jira. Uses Jira's transition API.")
    @PutMapping("/issues/{issueId}/status")
    public ResponseEntity<ApiResponse<JiraIssueResponse>> updateIssueStatus(
            @Parameter(description = "Issue ID", required = true, example = "1") @PathVariable Integer issueId,

            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        log.info("PUT /api/v1/jira/issues/{}/status - newStatus={}", issueId, newStatus);
        JiraIssueResponse response = issueService.updateIssueStatus(issueId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("Issue status updated", response));
    }

    /**
     * PUT /api/v1/jira/issues/{issueId}/assign
     * Assign issue to a user
     */
    @Operation(summary = "Assign issue to user")
    @PutMapping("/issues/{issueId}/assign")
    public ResponseEntity<ApiResponse<JiraIssueResponse>> assignIssue(
            @Parameter(description = "Issue ID", required = true, example = "1") @PathVariable Integer issueId,

            @RequestBody Map<String, Integer> body) {

        Integer assigneeId = body.get("assigneeId");
        log.info("PUT /api/v1/jira/issues/{}/assign - assigneeId={}", issueId, assigneeId);
        JiraIssueResponse response = issueService.assignIssue(issueId, assigneeId);
        return ResponseEntity.ok(ApiResponse.success("Issue assigned successfully", response));
    }
}
