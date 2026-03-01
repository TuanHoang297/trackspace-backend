package com.trackspace.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Project response payload")
public class ProjectResponse {

    @Schema(description = "Project ID")
    private Long id;

    @Schema(description = "Project name")
    private String projectName;

    @Schema(description = "Group ID")
    private Long groupId;

    @Schema(description = "Group name")
    private String groupName;

    @Schema(description = "Class ID")
    private Long classId;

    @Schema(description = "Class name")
    private String className;

    @Schema(description = "Whether project info has been filled")
    private Boolean hasProjectInfo;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated date")
    private LocalDateTime updatedAt;
}
