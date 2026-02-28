package com.trackspace.jira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraIssueRequest {

    @NotNull(message = "Project ID is required")
    private Integer projectId;

    private Integer sprintId;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    @NotBlank(message = "Summary is required")
    private String summary;

    private String description;

    private String priority;

    private Integer assigneeId;

    private LocalDate dueDate;
}
