package com.trackspace.student;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Student workspace info — group + project for a class")
public class WorkspaceResponse {

    @Schema(description = "Class ID")
    private Long classId;

    @Schema(description = "Class name")
    private String className;

    @Schema(description = "Group ID")
    private Long groupId;

    @Schema(description = "Group name")
    private String groupName;

    @Schema(description = "Project ID (null if no project)")
    private Long projectId;

    @Schema(description = "Project name (null if no project)")
    private String projectName;

    @Schema(description = "Whether the student is team leader of this group")
    @JsonProperty("isLeader")
    private boolean isLeader;
}
