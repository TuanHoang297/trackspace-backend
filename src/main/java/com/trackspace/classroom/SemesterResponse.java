package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Semester response payload")
public class SemesterResponse {

    @Schema(description = "Semester ID")
    private Long id;

    @Schema(description = "Semester name", example = "Spring 2026")
    private String name;

    @Schema(description = "Start date")
    private LocalDate startDate;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;
}
