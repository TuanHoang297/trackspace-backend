package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Class Response DTO
 * Response payload for class information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Class response payload")
public class ClassResponse {

    @Schema(description = "Class ID", example = "1")
    private Long id;

    @Schema(description = "Subject ID")
    private Long subjectId;

    @Schema(description = "Subject code", example = "SE101")
    private String subjectCode;

    @Schema(description = "Subject name", example = "Software Engineering")
    private String subjectName;

    @Schema(description = "Unique class code", example = "SE1801")
    private String classCode;

    @Schema(description = "Semester ID")
    private Long semesterId;

    @Schema(description = "Semester name", example = "Spring 2026")
    private String semesterName;

    @Schema(description = "Assigned lecturer ID")
    private Long lecturerId;

    @Schema(description = "Assigned lecturer full name")
    private String lecturerName;

    @Schema(description = "Assigned lecturer email")
    private String lecturerEmail;

    @Schema(description = "Total number of students enrolled")
    private Long totalStudents;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated date")
    private LocalDateTime updatedAt;
}
