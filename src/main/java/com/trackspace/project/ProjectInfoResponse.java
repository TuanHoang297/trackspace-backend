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
@Schema(description = "Project info response payload")
public class ProjectInfoResponse {

    @Schema(description = "Info record ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Project name")
    private String projectName;

    @Schema(description = "Project topic/title")
    private String topic;

    @Schema(description = "Background context")
    private String context;

    @Schema(description = "Problems to solve")
    private String problems;

    @Schema(description = "Main actors/stakeholders")
    private String primaryActors;

    @Schema(description = "Key functional requirements")
    private String functionalRequirements;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated date")
    private LocalDateTime updatedAt;
}
