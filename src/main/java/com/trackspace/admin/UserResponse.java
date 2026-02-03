package com.trackspace.admin;

import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Response DTO
 * Response for user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User information response")
public class UserResponse {

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

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;
}
