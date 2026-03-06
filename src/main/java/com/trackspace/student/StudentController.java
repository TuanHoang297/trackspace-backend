package com.trackspace.student;

import com.trackspace.auth.AuthService;
import com.trackspace.classroom.GroupMember;
import com.trackspace.classroom.GroupMemberRepository;
import com.trackspace.common.ApiResponse;
import com.trackspace.project.Project;
import com.trackspace.project.ProjectRepository;
import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Student Controller
 * APIs for student-specific operations
 */
@RestController
@RequestMapping("/api/student")
@Tag(name = "Student", description = "APIs for student workspace")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class StudentController {

    private final AuthService authService;
    private final GroupMemberRepository groupMemberRepository;
    private final ProjectRepository projectRepository;

    /**
     * Get all workspaces for the current student.
     * A student can belong to multiple classes → multiple groups → multiple
     * projects.
     */
    @GetMapping("/my-workspaces")
    @PreAuthorize("hasAnyRole('TEAMLEADER', 'TEAMMEMBER')")
    @Operation(summary = "Get student workspaces", description = "Returns all groups and projects the current student belongs to")
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getMyWorkspaces() {
        User user = authService.getCurrentUser();
        List<GroupMember> memberships = groupMemberRepository.findByMemberId(user.getId());

        List<WorkspaceResponse> workspaces = new ArrayList<>();
        for (GroupMember gm : memberships) {
            var group = gm.getGroup();
            var classroom = group.getClassroom();
            boolean isLeader = group.getTeamLeader() != null
                    && group.getTeamLeader().getId().equals(user.getId());

            // Find project for this group
            Project project = projectRepository.findByGroupIdAndDeletedFalse(group.getId()).orElse(null);

            workspaces.add(WorkspaceResponse.builder()
                    .classId(classroom.getId())
                    .className(classroom.getSubject() != null ? classroom.getSubject().getSubjectName() : null)
                    .groupId(group.getId())
                    .groupName(group.getGroupName())
                    .projectId(project != null ? project.getId() : null)
                    .projectName(project != null ? project.getProjectName() : null)
                    .isLeader(isLeader)
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách workspace thành công", workspaces));
    }
}
