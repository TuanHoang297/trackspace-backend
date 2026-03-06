package com.trackspace.classroom;

import com.trackspace.common.ApiResponse;
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
 * Group Controller
 * REST endpoints for student group management within a class
 */
@RestController
@RequestMapping("/api/classes/{classId}/groups")
@Tag(name = "Group", description = "APIs for student project group management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ==================== Group CRUD ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Create group", description = "Lecturer creates a new student group within a class")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @PathVariable Long classId,
            @Valid @RequestBody CreateGroupRequest request) {
        GroupResponse response = groupService.createGroup(classId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Tạo nhóm thành công", response)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Get all groups in class", description = "Get overview of all groups assigned within a class")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAllGroups(
            @PathVariable Long classId) {
        List<GroupResponse> groups = groupService.getAllGroupsByClass(classId);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách nhóm thành công", groups)
        );
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Get group by ID", description = "Get detailed information of a specific group")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroupById(
            @PathVariable Long classId,
            @PathVariable Long groupId) {
        GroupResponse response = groupService.getGroupById(classId, groupId);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin nhóm thành công", response)
        );
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Update group", description = "Lecturer updates group name or description")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable Long classId,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupResponse response = groupService.updateGroup(classId, groupId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật nhóm thành công", response)
        );
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Delete group", description = "Lecturer soft-deletes a group")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @PathVariable Long classId,
            @PathVariable Long groupId) {
        groupService.deleteGroup(classId, groupId);
        return ResponseEntity.ok(
                ApiResponse.success("Xóa nhóm thành công", null)
        );
    }

    // ==================== Leader Assignment ====================

    @PutMapping("/{groupId}/leader")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Assign team leader", description = "Student must be a member of the group. Previous leader is demoted to TEAMMEMBER.")
    public ResponseEntity<ApiResponse<GroupResponse>> assignLeader(
            @PathVariable Long classId,
            @PathVariable Long groupId,
            @Valid @RequestBody AssignLeaderRequest request) {
        GroupResponse response = groupService.assignLeader(classId, groupId, request.getStudentId());
        return ResponseEntity.ok(
                ApiResponse.success("Phân công Team Leader thành công", response)
        );
    }

    // ==================== Member Management ====================

    @GetMapping("/{groupId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'TEAMLEADER', 'TEAMMEMBER')")
    @Operation(summary = "Get group members", description = "Get list of all members in a group")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getGroupMembers(
            @PathVariable Long classId,
            @PathVariable Long groupId) {
        List<GroupMemberResponse> members = groupService.getGroupMembers(classId, groupId);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách thành viên nhóm thành công", members)
        );
    }

    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Add member to group", description = "Student must be enrolled in the class and not already in another group.")
    public ResponseEntity<ApiResponse<GroupMemberResponse>> addMember(
            @PathVariable Long classId,
            @PathVariable Long groupId,
            @Valid @RequestBody AddGroupMemberRequest request) {
        GroupMemberResponse response = groupService.addMember(classId, groupId, request.getStudentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Thêm thành viên vào nhóm thành công", response)
        );
    }

    @DeleteMapping("/{groupId}/members/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Remove member from group", description = "Lecturer removes a student from a group")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long classId,
            @PathVariable Long groupId,
            @PathVariable Long studentId) {
        groupService.removeMember(classId, groupId, studentId);
        return ResponseEntity.ok(
                ApiResponse.success("Xóa thành viên khỏi nhóm thành công", null)
        );
    }
}
