package com.trackspace.admin;

import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update User Request DTO
 * Request body for admin to update user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user request payload for admin")
public class UpdateUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Schema(description = "User full name", example = "Nguyễn Văn A")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @NotNull(message = "Role không được để trống")
    @Schema(description = "User role", example = "LECTURER")
    private User.Role role;

    @Schema(description = "Student code (for students only)", example = "SE171234")
    private String studentCode;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Schema(description = "New password (optional, leave blank to keep current)", example = "newPassword123")
    private String password;
}
