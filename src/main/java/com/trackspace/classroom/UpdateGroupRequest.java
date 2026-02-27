package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Group Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update group request payload")
public class UpdateGroupRequest {

    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    @Schema(description = "New group name", example = "Nhóm 1 - Updated")
    private String groupName;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    @Schema(description = "New group description")
    private String description;
}
