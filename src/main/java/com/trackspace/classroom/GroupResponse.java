package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Group Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Group response payload")
public class GroupResponse {

    @Schema(description = "Group ID")
    private Long id;

    @Schema(description = "Group name", example = "Nhóm 1")
    private String groupName;

    @Schema(description = "Group description / project topic")
    private String description;

    @Schema(description = "Class ID this group belongs to")
    private Long classId;

    @Schema(description = "Class name")
    private String className;

    @Schema(description = "Team Leader user ID")
    private Long teamLeaderId;

    @Schema(description = "Team Leader full name")
    private String teamLeaderName;

    @Schema(description = "Team Leader email")
    private String teamLeaderEmail;

    @Schema(description = "Total number of members in this group")
    private Long totalMembers;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated date")
    private LocalDateTime updatedAt;
}
