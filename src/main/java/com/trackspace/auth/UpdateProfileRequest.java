package com.trackspace.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for updating current user's profile.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String studentCode;
}
