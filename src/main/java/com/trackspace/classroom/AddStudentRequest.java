package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Add Student Request DTO
 * Request body for adding a student to a class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add student to class request payload")
public class AddStudentRequest {

    @NotNull(message = "Student ID không được để trống")
    @Schema(description = "Student user ID to add to this class", example = "5")
    private Long studentId;
}
