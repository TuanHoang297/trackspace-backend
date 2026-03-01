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

    @NotBlank(message = "Tên lớp không được để trống")
    @Size(max = 255, message = "Tên lớp không được vượt quá 255 ký tự")
    @Schema(description = "Class name", example = "Software Engineering")
    private String className;

    @NotBlank(message = "Mã lớp không được để trống")
    @Size(max = 50, message = "Mã lớp không được vượt quá 50 ký tự")
    @Schema(description = "Unique class code", example = "SE1801")
    private String classCode;

    @NotBlank(message = "Học kỳ không được để trống")
    @Size(max = 50, message = "Học kỳ không được vượt quá 50 ký tự")
    @Schema(description = "Semester", example = "Spring 2026")
    private String semester;

    @Schema(description = "Lecturer user ID to assign to this class", example = "5")
    private Long lecturerId;
}
