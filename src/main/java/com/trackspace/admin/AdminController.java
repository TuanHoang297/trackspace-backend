package com.trackspace.admin;

import com.trackspace.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final ExcelImportService excelImportService;

    /**
     * Create user account
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
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Admin deletes a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Xóa người dùng thành công", null)
        );
    }

    /**
     * Import users from Excel file
     */
    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import users from Excel", description = "Admin imports multiple users from .xlsx file")
    public ResponseEntity<ApiResponse<ImportResult>> importUsers(@RequestParam("file") MultipartFile file) {
        ImportResult result = excelImportService.importUsers(file);
        String msg = String.format("Import hoàn tất: %d thành công, %d thất bại",
                result.getSuccessCount(), result.getFailedCount());
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    /**
     * Download Excel template for user import
     */
    @GetMapping("/users/import-template")
    @Operation(summary = "Download import template", description = "Download Excel template for user import")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_users_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }
}

