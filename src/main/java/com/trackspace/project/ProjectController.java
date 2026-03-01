package com.trackspace.project;

import com.trackspace.auth.AuthService;
import com.trackspace.common.ApiResponse;
import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project Controller
 * REST endpoints for project and project info management
 *
 * Base paths:
 *   /api/groups/{groupId}/project        — project scoped to a group
 *   /api/projects/{projectId}            — direct project access
 *   /api/projects/{projectId}/info       — project info (SRS input data)
 *   /api/classes/{classId}/projects      — list all projects in a class
 */
@RestController
@Tag(name = "Project", description = "APIs for project and project info management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AuthService authService;

    // ==================== Project CRUD ====================

    @PostMapping("/api/groups/{groupId}/project")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER')")
    @Operation(summary = "Create project for a group", description = "Creates one project for the group. Each group can only have one project.")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateProjectRequest request) {
        User currentUser = authService.getCurrentUser();
        ProjectResponse response = projectService.createProject(groupId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo project thành công", response));
    }

    @GetMapping("/api/groups/{groupId}/project")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER', 'TEAMMEMBER')")
    @Operation(summary = "Get project by group")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectByGroup(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectByGroup(groupId)));
    }

    @GetMapping("/api/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER', 'TEAMMEMBER')")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectById(projectId)));
    }

    @GetMapping("/api/classes/{classId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Get all projects in a class", description = "Admin or Lecturer can view all projects within a class.")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectsByClass(
            @PathVariable Long classId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectsByClass(classId)));
    }

    @PutMapping("/api/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER')")
    @Operation(summary = "Update project name")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        User currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật project thành công",
                        projectService.updateProject(projectId, request, currentUser)));
    }

    @DeleteMapping("/api/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Delete (soft-delete) project")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long projectId) {
        User currentUser = authService.getCurrentUser();
        projectService.deleteProject(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Xóa project thành công", null));
    }

    // ==================== Project Info ====================

    @PutMapping("/api/projects/{projectId}/info")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER', 'TEAMMEMBER')")
    @Operation(summary = "Create or update project info",
            description = "Upsert: creates info if not exists, updates if already exists. Used to fill in data for SRS generation.")
    public ResponseEntity<ApiResponse<ProjectInfoResponse>> saveProjectInfo(
            @PathVariable Long projectId,
            @RequestBody ProjectInfoRequest request) {
        User currentUser = authService.getCurrentUser();
        ProjectInfoResponse response = projectService.saveProjectInfo(projectId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lưu thông tin project thành công", response));
    }

    @GetMapping("/api/projects/{projectId}/info")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER', 'TEAMMEMBER')")
    @Operation(summary = "Get project info")
    public ResponseEntity<ApiResponse<ProjectInfoResponse>> getProjectInfo(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectInfo(projectId)));
    }

    @DeleteMapping("/api/projects/{projectId}/info")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER')")
    @Operation(summary = "Delete project info")
    public ResponseEntity<ApiResponse<Void>> deleteProjectInfo(
            @PathVariable Long projectId) {
        User currentUser = authService.getCurrentUser();
        projectService.deleteProjectInfo(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Xóa thông tin project thành công", null));
    }
}
