package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Assign Lecturer Request DTO
 * Request body for admin to assign a lecturer to a class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assign lecturer to class request payload")
public class AssignLecturerRequest {

    @NotNull(message = "Lecturer ID không được để trống")
    @Schema(description = "Lecturer user ID", example = "2")
    private Long lecturerId;
}
