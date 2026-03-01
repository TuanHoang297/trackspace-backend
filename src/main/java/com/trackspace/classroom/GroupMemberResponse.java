package com.trackspace.classroom;

import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Group Member Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Group member response payload")
public class GroupMemberResponse {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Full name")
    private String fullName;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Role (TEAMLEADER or TEAMMEMBER)")
    private User.Role role;

    @Schema(description = "Is this member the team leader of the group")
    private Boolean isLeader;

    @Schema(description = "Date joined the group")
    private LocalDateTime joinedAt;
}
