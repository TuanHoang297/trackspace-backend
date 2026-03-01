package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Student In Class Response DTO
 * Response payload for student info within a class context
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Student in class response payload")
public class StudentInClassResponse {

    @Schema(description = "Enrollment record ID")
    private Long enrollmentId;

    @Schema(description = "Student user ID")
    private Long studentId;

    @Schema(description = "Student full name")
    private String fullName;

    @Schema(description = "Student email")
    private String email;

    @Schema(description = "Student code (MSSV)")
    private String studentCode;

    @Schema(description = "Group ID the student belongs to (null if not in a group)")
    private Long groupId;

    @Schema(description = "Group name the student belongs to (null if not in a group)")
    private String groupName;

    @Schema(description = "Enrollment date")
    private LocalDateTime enrolledAt;
}
