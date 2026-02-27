package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Assign Leader Request DTO
 * Used by Lecturer to assign a Team Leader for a group
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assign team leader request payload")
public class AssignLeaderRequest {

    @NotNull(message = "Student ID không được để trống")
    @Schema(description = "Student user ID to be promoted to Team Leader", example = "5")
    private Long studentId;
}
