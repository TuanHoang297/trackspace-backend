package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Subject ID", example = "1")
    private Long subjectId;

    @Schema(description = "Semester ID", example = "1")
    private Long semesterId;

    @Schema(description = "Active status of the class", example = "true")
    private Boolean active;

    @Schema(description = "Lecturer user ID to assign to this class", example = "5")
    private Long lecturerId;
}
