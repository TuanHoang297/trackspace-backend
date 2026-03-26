package com.trackspace.github.controller;

import com.trackspace.common.ApiResponse;
import com.trackspace.github.dto.*;
import com.trackspace.github.service.CommitService;
import com.trackspace.github.service.ConnectionService;
import com.trackspace.github.service.GitHubApiClient;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for GitHub Integration
 *
 * Provides endpoints for:
 * - Connecting/disconnecting a GitHub repository to a project
 * - Checking connection status
 * - Syncing commits from GitHub
 * - Retrieving commits and statistics
 */
@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitHub Integration", description = "Endpoints for connecting GitHub repositories to projects, syncing commits, and viewing contribution statistics")
public class GitHubController {

  private final ConnectionService connectionService;
  private final CommitService commitService;
  private final GitHubApiClient gitHubApiClient;

  // ==================== CONNECTION ENDPOINTS ====================

  /**
   * POST /api/v1/github/connect
   * Connect a GitHub repository to a project
   */
  @Operation(summary = "Connect GitHub repository", description = """
      Connects a GitHub repository to a TrackSpace project.
      - Validates the provided personal access token with the GitHub API.
      - If branchName is omitted, the default branch of the repository is used.
      - Only one repository can be connected per project at a time.
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Repository connected successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
          {
            "success": true,
            "message": "GitHub repository connected successfully",
            "data": {
              "connectionId": 1,
              "projectId": 42,
              "repositoryUrl": "https://github.com/org/repo",
              "branchName": "main",
              "connectionStatus": "CONNECTED",
              "lastSyncAt": null,
              "totalCommits": 0
            }
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request (invalid token, invalid URL, or already connected)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": false,
            "message": "Invalid GitHub token or no access to repository",
            "data": null
          }
          """)))
  })
  @PostMapping("/connect")
  public ResponseEntity<ApiResponse<ConnectionStatusResponse>> connectRepository(
      @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Connection details: project ID, GitHub repo URL, and personal access token", required = true, content = @Content(schema = @Schema(implementation = ConnectionRequest.class), examples = @ExampleObject(value = """
          {
            "projectId": 42,
            "repositoryUrl": "https://github.com/org/my-repo",
            "accessToken": "ghp_xxxxxxxxxxxxxxxxxxxx",
            "branchName": "main"
          }
          """))) ConnectionRequest request) {

    log.info("POST /api/v1/github/connect - projectId={}", request.getProjectId());
    ConnectionStatusResponse response = connectionService.connectRepository(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success("GitHub repository connected successfully", response));
  }

  /**
   * GET /api/v1/github/status/{projectId}
   * Get the connection status for a project
   */
  @Operation(summary = "Get connection status", description = "Returns the current GitHub connection status for the given project, including repository URL, branch, last sync time, and total commits.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Connection status returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
          {
            "success": true,
            "message": "Success",
            "data": {
              "connectionId": 1,
              "projectId": 42,
              "repositoryUrl": "https://github.com/org/repo",
              "branchName": "main",
              "connectionStatus": "CONNECTED",
              "lastSyncAt": "2025-02-01T10:00:00Z",
              "totalCommits": 150
            }
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No GitHub connection found for the given project")
  })
  @GetMapping("/status/{projectId}")
  public ResponseEntity<ApiResponse<ConnectionStatusResponse>> getConnectionStatus(
      @Parameter(description = "ID of the project to check connection for", required = true, example = "42") @PathVariable("projectId") Integer projectId) {

    log.debug("GET /api/v1/github/status/{}", projectId);
    ConnectionStatusResponse response = connectionService.getConnectionStatus(projectId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * GET /api/v1/github/connections/{projectId}
   * Get ALL connections for a project (multi-repo: FE + BE)
   */
  @Operation(summary = "Get all connections", description = "Returns all GitHub connections for a project, supporting multiple repositories (FE + BE).")
  @GetMapping("/connections/{projectId}")
  public ResponseEntity<ApiResponse<List<ConnectionStatusResponse>>> getConnections(
      @Parameter(description = "ID of the project", required = true) @PathVariable("projectId") Integer projectId) {

    log.debug("GET /api/v1/github/connections/{}", projectId);
    List<ConnectionStatusResponse> connections = connectionService.getConnections(projectId);
    return ResponseEntity.ok(ApiResponse.success(connections));
  }

  /**
   * DELETE /api/v1/github/disconnect/{projectId}
   * Disconnect a GitHub repository from a project
   */
  @Operation(summary = "Disconnect GitHub repository", description = "Disconnects the GitHub repository from a project. The stored access token is cleared and the connection status is set to DISCONNECTED. Existing commit data is preserved.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repository disconnected successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No GitHub connection found for the given project")
  })
  @DeleteMapping("/disconnect/{projectId}")
  public ResponseEntity<ApiResponse<Void>> disconnectRepository(
      @Parameter(description = "ID of the project to disconnect", required = true, example = "42") @PathVariable("projectId") Integer projectId) {

    log.info("DELETE /api/v1/github/disconnect/{}", projectId);
    connectionService.disconnectRepository(projectId);
    return ResponseEntity.ok(ApiResponse.success("GitHub repository disconnected successfully", null));
  }

  /**
   * DELETE /api/v1/github/disconnect/connection/{connectionId}
   * Disconnect a SINGLE GitHub repository connection
   */
  @Operation(summary = "Disconnect single GitHub repository", description = "Disconnects a single GitHub repository by connection ID. Only affects the specified connection, other repos remain connected.")
  @DeleteMapping("/disconnect/connection/{connectionId}")
  public ResponseEntity<ApiResponse<Void>> disconnectSingleRepository(
      @Parameter(description = "ID of the connection to disconnect", required = true) @PathVariable("connectionId") Integer connectionId) {

    log.info("DELETE /api/v1/github/disconnect/connection/{}", connectionId);
    connectionService.disconnectSingleRepository(connectionId);
    return ResponseEntity.ok(ApiResponse.success("GitHub repository disconnected successfully", null));
  }

  // ==================== COMMIT ENDPOINTS ====================

  /**
   * POST /api/v1/github/sync
   * Trigger a sync of commits from GitHub
   */
  @Operation(summary = "Sync commits from GitHub", description = """
      Fetches new commits from GitHub and saves them to the database.
      - If `since` is not provided, syncs from the last sync time or 30 days ago if never synced.
      - If `branch` is not provided, uses the branch configured in the connection.
      - Duplicate commits (same SHA) are skipped automatically.
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sync completed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": true,
            "message": "Sync completed",
            "data": {
              "commitsSynced": 12,
              "commitsSkipped": 3,
              "lastSyncAt": "2025-02-26T06:00:00Z",
              "message": "Successfully synced 12 commits"
            }
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No GitHub connection found for the given project")
  })
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<Map<String, Object>>> syncCommits(
      @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Sync parameters: project ID, optional since timestamp, optional branch override", required = true, content = @Content(schema = @Schema(implementation = SyncRequest.class), examples = @ExampleObject(value = """
          {
            "projectId": 42,
            "since": "2025-01-01T00:00:00Z",
            "branch": "develop"
          }
          """))) SyncRequest request) {

    log.info("POST /api/v1/github/sync - projectId={}", request.getProjectId());
    Map<String, Object> result = commitService.syncCommits(request);
    return ResponseEntity.ok(ApiResponse.success("Sync completed", result));
  }

  /**
   * GET /api/v1/github/commits/{projectId}
   * Get commits for a project with optional filters
   */
  @Operation(summary = "Get commits for a project", description = """
      Returns commits for a project. Supports optional filters:
      - `userId`: filter by a specific team member
      - `since` / `until`: filter by date range (ISO-8601 format, e.g. `2025-01-01T00:00:00Z`)
      - `branch`: filter by branch name (matches commits that belong to this branch)
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of commits returned", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": true,
            "message": "Success",
            "data": [
              {
                "commitId": 1,
                "commitSha": "abc123def456",
                "commitMessage": "feat: add user authentication",
                "authorName": "Tuan Hoang",
                "authorEmail": "tuan@example.com",
                "authorId": 7,
                "commitDate": "2025-02-20T08:30:00Z",
                "filesChanged": 5,
                "linesAdded": 120,
                "linesDeleted": 30,
                "branchName": "main",
                "linkedIssueId": null
              }
            ]
          }
          """)))
  })
  @GetMapping("/commits/{projectId}")
  public ResponseEntity<ApiResponse<List<CommitResponse>>> getCommits(
      @Parameter(description = "ID of the project", required = true, example = "42") @PathVariable("projectId") Integer projectId,

      @Parameter(description = "Filter by connection ID (optional) — scopes to a single repo") @RequestParam(required = false) Integer connectionId,

      @Parameter(description = "Filter by user ID (optional)", example = "7") @RequestParam(required = false) Integer userId,

      @Parameter(description = "Start of date range, inclusive (ISO-8601)", example = "2025-01-01T00:00:00Z") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,

      @Parameter(description = "End of date range, inclusive (ISO-8601)", example = "2025-02-28T23:59:59Z") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant until,

      @Parameter(description = "Filter by branch name (optional)", example = "main") @RequestParam(required = false) String branch) {

    log.debug("GET /api/v1/github/commits/{} - connectionId={}, userId={}, since={}, until={}, branch={}", projectId,
        connectionId, userId, since,
        until, branch);
    List<CommitResponse> commits = commitService.getCommits(projectId, connectionId, userId, since, until, branch);
    return ResponseEntity.ok(ApiResponse.success(commits));
  }

  /**
   * GET /api/v1/github/commits/{projectId}/by-branch
   * Get commits for a specific branch directly from GitHub API (real-time).
   * Unlike the regular /commits endpoint which filters by DB branch_name,
   * this fetches directly from GitHub to show all commits on any branch.
   */
  @Operation(summary = "Get commits by branch (real-time from GitHub)")
  @GetMapping("/commits/{projectId}/by-branch")
  public ResponseEntity<ApiResponse<List<CommitResponse>>> getCommitsByBranch(
      @PathVariable("projectId") Integer projectId,
      @RequestParam("connectionId") Integer connectionId,
      @RequestParam("branch") String branch) {
    List<CommitResponse> commits = commitService.getCommitsByBranch(projectId, connectionId, branch);
    return ResponseEntity.ok(ApiResponse.success(commits));
  }

  /**
   * GET /api/v1/github/stats/{projectId}
   * Get contribution statistics for a project
   */
  @Operation(summary = "Get contribution statistics", description = """
      Returns contribution statistics per member for a project.
      - If `userId` is provided, returns stats for that specific user only.
      - If `userId` is omitted, returns stats for **all** team members.
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics returned", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": true,
            "message": "Success",
            "data": [
              {
                "userId": 7,
                "userName": "Tuan Hoang",
                "totalCommits": 42,
                "totalLinesAdded": 3500,
                "totalLinesDeleted": 800,
                "totalChanges": 4300,
                "lastCommitAt": "2025-02-25T15:00:00Z"
              }
            ]
          }
          """)))
  })
  @GetMapping("/stats/{projectId}")
  public ResponseEntity<ApiResponse<List<StatsResponse>>> getStats(
      @Parameter(description = "ID of the project", required = true, example = "42") @PathVariable("projectId") Integer projectId,

      @Parameter(description = "Filter by connection ID — scopes to a single repo") @RequestParam(required = false) Integer connectionId,

      @Parameter(description = "Filter by user ID — omit to get all members", example = "7") @RequestParam(required = false) Integer userId) {

    log.debug("GET /api/v1/github/stats/{} - connectionId={}, userId={}", projectId, connectionId, userId);
    List<StatsResponse> stats = commitService.getStats(projectId, connectionId, userId);
    return ResponseEntity.ok(ApiResponse.success(stats));
  }

  // ==================== BRANCH ENDPOINTS ====================

  /**
   * GET /api/v1/github/branches/{projectId}
   * Get all branches for a connected repository
   */
  @Operation(summary = "Get branches", description = "Returns all branches from the connected GitHub repository for the given project.")
  @GetMapping("/branches/{projectId}")
  public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranches(
      @Parameter(description = "ID of the project", required = true, example = "42") @PathVariable("projectId") Integer projectId,
      @Parameter(description = "Filter by connection ID — scopes to a single repo") @RequestParam(required = false) Integer connectionId) {

    // Get connection entity (has token + URL)
    var connection = connectionId != null
        ? connectionService.getConnectionById(connectionId)
        : connectionService.getConnection(projectId);

    // Parse owner/repo from URL
    String cleanUrl = connection.getRepositoryUrl().replace(".git", "").replace("https://github.com/", "");
    String[] parts = cleanUrl.split("/");
    String owner = parts[0];
    String repo = parts[1];

    var ghBranches = gitHubApiClient.fetchBranches(owner, repo, connection.getAccessTokenEncrypted());

    List<BranchResponse> branches = ghBranches.stream()
        .map(b -> BranchResponse.builder()
            .name(b.getName())
            .isProtected(b.getIsProtected())
            .lastCommitSha(b.getCommit() != null ? b.getCommit().getSha() : null)
            .build())
        .toList();

    return ResponseEntity.ok(ApiResponse.success(branches));
  }
}
