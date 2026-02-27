package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Group Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create group request payload")
public class CreateGroupRequest {

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    @Schema(description = "Group name", example = "Nhóm 1")
    private String groupName;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    @Schema(description = "Group description / project topic", example = "Xây dựng hệ thống quản lý bán hàng")
    private String description;
}
