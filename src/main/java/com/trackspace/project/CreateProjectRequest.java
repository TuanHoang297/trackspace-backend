package com.trackspace.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new project for a group")
public class CreateProjectRequest {

    @NotBlank(message = "Tên project không được để trống")
    @Size(max = 255, message = "Tên project không được vượt quá 255 ký tự")
    @Schema(description = "Project name", example = "TrackSpace Web App")
    private String projectName;
}
