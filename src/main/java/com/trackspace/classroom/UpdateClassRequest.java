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

    @Size(max = 100, message = "Tên lớp không được vượt quá 100 ký tự")
    @Schema(description = "New class name", example = "SE1801-Updated")
    private String className;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    @Schema(description = "New class description", example = "Updated description")
    private String description;

    @Schema(description = "Active status of the class", example = "true")
    private Boolean active;
}
