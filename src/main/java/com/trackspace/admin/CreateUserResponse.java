package com.trackspace.admin;

import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create User Response DTO
 * Response after admin creates a user account
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for user creation by admin")
public class CreateUserResponse {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "User full name")
    private String fullName;

    @Schema(description = "User role")
    private User.Role role;

    @Schema(description = "User active status")
    private Boolean active;

    @Schema(description = "Response message")
    private String message;
}
