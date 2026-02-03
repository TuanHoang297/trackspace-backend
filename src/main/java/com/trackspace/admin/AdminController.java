package com.trackspace.admin;

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
 * Admin Controller
 * Handles admin operations for user management
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "APIs for admin operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Create user account
     *
     * @param request User creation request containing email, password, fullName, and role
     * @return Created user details
     */
    @PostMapping("/users")
    @Operation(summary = "Create user account", description = "Admin creates account for lecturer, teamleader, or teammember")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(response.getMessage(), response)
        );
    }

    /**
     * Get all users in the system
     *
     * @return List of all users with their details
     */
    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Admin retrieves all users in the system")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách người dùng thành công", users)
        );
    }

    /**
     * Update user status (activate/deactivate)
     *
     * @param userId User ID to update
     * @param request Status update request
     * @return Updated user details
     */
    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update user status", description = "Admin activates or deactivates a user account")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserResponse response = adminService.updateUserStatus(userId, request.getActive());
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật trạng thái người dùng thành công", response)
        );
    }

    /**
     * Delete user account
     *
     * @param userId User ID to delete
     * @return Success response
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Admin deletes a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Xóa người dùng thành công", null)
        );
    }
}
