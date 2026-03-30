package com.trackspace.analytics;

import com.trackspace.common.ApiResponse;
import com.trackspace.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller – Analytics Module
 *
 * Base path: /api/v1/analytics
 *
 * Endpoints:
 *  POST /recalculate/{projectId}               → trigger (re)calculation
 *  GET  /contributions/project/{projectId}     → all members' scores
 *  GET  /contributions/user/{userId}           → single member's score
 *  GET  /dashboard/{projectId}                 → project dashboard
 *  GET  /heatmap/{userId}                      → activity heatmap
 *  GET  /issues/{projectId}                    → detected anomalies
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Impact & Consistency Score — contribution tracking for projects")
public class ContributionController {

    private final ContributionService contributionService;
    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────────────────
    // Recalculate
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Recalculate contributions for a project (V2)",
            description = """
                    Triggers a full recalculation of the contribution score for every
                    member of the project.  Persists results to the database.

                    V2 Score formula:
                    - **Code Score (40%)**: Per-file weighted lines added (Logic=1.0, UI=0.5, Config=0.0),
                      normalized against group maximum.
                    - **Task Score (40%)**: tasksCompleted / tasksAssigned (Jira).
                    - **Consistency Score (20%)**: activeDays / (projectWeeks × 3),
                      based on 3 active days/week expectation.
                    """
    )
    @PostMapping("/recalculate/{projectId}")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> recalculate(
            @Parameter(description = "Project ID") @PathVariable("projectId") Integer projectId) {
        contributionService.checkProjectAccess(projectId);
        log.info("Recalculating V2 contributions for project {}", projectId);
        List<ContributionResponse> result = contributionService.recalculate(projectId);
        return ResponseEntity.ok(ApiResponse.success("Recalculation complete", result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contributions
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get all members' contribution scores for a project",
            description = "Returns cached scores sorted by contributionScore descending. " +
                          "Triggers recalculation on first access."
    )
    @GetMapping("/contributions/project/{projectId}")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getByProject(
            @PathVariable("projectId") Integer projectId) {
        contributionService.checkProjectAccess(projectId);
        return ResponseEntity.ok(ApiResponse.success(contributionService.getByProject(projectId)));
    }

    @Operation(
            summary = "Get a single user's contribution score in a project",
            description = "Returns the detailed breakdown for one member."
    )
    @GetMapping("/contributions/user/{userId}")
    public ResponseEntity<ApiResponse<ContributionResponse>> getByUser(
            @Parameter(description = "User ID") @PathVariable("userId") Long userId,
            @Parameter(description = "Project ID", required = true)
            @RequestParam("projectId") Integer projectId) {
        return ResponseEntity.ok(ApiResponse.success(
                contributionService.getByUser(projectId, userId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Project analytics dashboard",
            description = """
                    Returns aggregate project statistics plus per-member data for charts:
                    - **Radar / Bar chart**: `tasksCompleted` vs `githubImpactScore` per member.
                    - **Anomaly list**: human-readable issues (inactive, low score, overdue, churn).
                    """
    )
    @GetMapping("/dashboard/{projectId}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @PathVariable("projectId") Integer projectId) {
        contributionService.checkProjectAccess(projectId);
        return ResponseEntity.ok(ApiResponse.success(contributionService.getDashboard(projectId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heatmap
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Activity heatmap for a user in a project",
            description = """
                    Returns daily commit counts and lines-added for the given user.
                    Suitable for rendering a GitHub-style contribution calendar.
                    """
    )
    @GetMapping("/heatmap/{userId}")
    public ResponseEntity<ApiResponse<HeatmapResponse>> getHeatmap(
            @Parameter(description = "User ID") @PathVariable("userId") Long userId,
            @Parameter(description = "Project ID", required = true)
            @RequestParam("projectId") Integer projectId) {
        return ResponseEntity.ok(ApiResponse.success(
                contributionService.getHeatmap(projectId, userId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Issue Detection
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Detect contribution anomalies in a project",
            description = """
                    Scans all member scores and flags:
                    - **INACTIVE** – no commit in ≥ 3 days
                    - **LOW_CONTRIBUTION** – score < 20% of project average
                    - **OVERDUE_TASKS** – Jira tasks past their due date
                    - **HIGH_CHURN** – code churn rate > 1.5 (deleted > added)
                    - **HIGH_REWORK** – caused ≥ 2 bug-fix commits from teammates
                    """
    )
    @GetMapping("/issues/{projectId}")
    public ResponseEntity<ApiResponse<IssueDetectionResponse>> detectIssues(
            @PathVariable("projectId") Integer projectId) {
        contributionService.checkProjectAccess(projectId);
        return ResponseEntity.ok(ApiResponse.success(
                contributionService.detectIssues(projectId)));
    }
}
