package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Class Request DTO
 * Request body for admin to create a new class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create class request payload")
public class CreateClassRequest {

    @Schema(description = "Subject ID", example = "1")
    private Long subjectId;

    @NotBlank(message = "Mã môn không được để trống")
    @Size(max = 50, message = "Mã môn không được vượt quá 50 ký tự")
    @Schema(description = "Unique class code", example = "SE1801")
    private String classCode;

    @Schema(description = "Semester ID", example = "1")
    private Long semesterId;

    @Schema(description = "Lecturer user ID to assign to this class", example = "5")
    private Long lecturerId;
}
