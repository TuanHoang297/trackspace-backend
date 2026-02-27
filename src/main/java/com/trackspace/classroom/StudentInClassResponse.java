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

    @Schema(description = "ClassStudent record ID")
    private Long enrollmentId;

    @Schema(description = "Student user ID")
    private Long studentId;

    @Schema(description = "Student full name")
    private String fullName;

    @Schema(description = "Student email")
    private String email;

    @Schema(description = "Enrollment date")
    private LocalDateTime enrolledAt;
}
