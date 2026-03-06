package com.trackspace.classroom;

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
@Schema(description = "Subject response payload")
public class SubjectResponse {

    @Schema(description = "Subject ID")
    private Long id;

    @Schema(description = "Subject code", example = "SE101")
    private String subjectCode;

    @Schema(description = "Subject name", example = "Software Engineering")
    private String subjectName;

    @Schema(description = "Subject description")
    private String description;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;
}
