package com.trackspace.auth;

import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO
 * Response body for authentication operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication response")
public class AuthResponse {

    @Schema(description = "JWT access token")
    private String token;

    @Builder.Default
    @Schema(description = "Token type", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "User full name")
    private String fullName;

    @Schema(description = "User role")
    private User.Role role;

    @Schema(description = "Success message")
    private String message;

    /**
     * Constructor without message
     */
    public AuthResponse(String token, Long userId, String email, String fullName, User.Role role) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
