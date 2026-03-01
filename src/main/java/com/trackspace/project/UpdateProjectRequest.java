package com.trackspace.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update project name")
public class UpdateProjectRequest {

    @Size(max = 255, message = "Tên project không được vượt quá 255 ký tự")
    @Schema(description = "New project name", example = "TrackSpace Web App v2")
    private String projectName;
}
