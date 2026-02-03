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
 * Create User Request DTO
 * Request body for admin to create user account
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create user request payload for admin")
public class CreateUserRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Schema(description = "User email", example = "lecturer@example.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Schema(description = "User password", example = "password123")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Schema(description = "User full name", example = "Nguyễn Văn A")
    private String fullName;

    @NotNull(message = "Role không được để trống")
    @Schema(description = "User role (LECTURER, TEAMLEADER, TEAMMEMBER)", example = "LECTURER")
    private User.Role role;
}
