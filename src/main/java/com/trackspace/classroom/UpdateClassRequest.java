package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Class Request DTO
 * Request body for admin to update an existing class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update class request payload")
public class UpdateClassRequest {

    @Size(max = 255, message = "Tên lớp không được vượt quá 255 ký tự")
    @Schema(description = "New class name", example = "Software Engineering Updated")
    private String className;

    @Size(max = 50, message = "Học kỳ không được vượt quá 50 ký tự")
    @Schema(description = "Semester", example = "Fall 2026")
    private String semester;

    @Schema(description = "Active status of the class", example = "true")
    private Boolean active;

    @Schema(description = "Lecturer user ID to assign to this class", example = "5")
    private Long lecturerId;
}
