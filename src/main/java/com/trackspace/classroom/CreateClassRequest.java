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
    @Size(max = 100, message = "Tên lớp không được vượt quá 100 ký tự")
    @Schema(description = "Class name", example = "SE1801")
    private String className;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    @Schema(description = "Class description", example = "Software Engineering class - Semester 1 2026")
    private String description;
}
