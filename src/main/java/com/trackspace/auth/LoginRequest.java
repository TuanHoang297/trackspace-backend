package com.trackspace.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO
 * Request body for user login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request payload")
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Schema(description = "User password", example = "password123")
    private String password;
}
