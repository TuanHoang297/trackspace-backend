package com.trackspace.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update project info for SRS generation")
public class ProjectInfoRequest {

    @Schema(description = "Project topic/title", example = "Student Progress Tracking System")
    private String topic;

    @Schema(description = "Background context of the project", example = "Universities lack a unified tool to track student project progress...")
    private String context;

    @Schema(description = "Problems the project solves", example = "Difficulty in monitoring individual contributions...")
    private String problems;

    @Schema(description = "Main actors/stakeholders", example = "Admin, Lecturer, Team Leader, Team Member")
    private String primaryActors;

    @Schema(description = "Key functional requirements", example = "1. Manage classes and groups\\n2. Track Jira tasks...")
    private String functionalRequirements;
}
