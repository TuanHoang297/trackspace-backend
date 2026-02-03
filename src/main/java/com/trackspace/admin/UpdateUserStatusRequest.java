package com.trackspace.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update User Status Request DTO
 * Request body for updating user status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user status request")
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "User active status", example = "true")
    private Boolean active;
}
