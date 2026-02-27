package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Add Group Member Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add member to group request payload")
public class AddGroupMemberRequest {

    @NotNull(message = "Student ID không được để trống")
    @Schema(description = "Student user ID to add to this group", example = "6")
    private Long studentId;
}
