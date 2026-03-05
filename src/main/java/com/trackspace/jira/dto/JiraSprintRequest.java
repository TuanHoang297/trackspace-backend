package com.trackspace.jira.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraSprintRequest {
    @NotNull
    private Integer projectId;

    @NotNull
    @Size(min = 1, max = 255)
    private String name;

    private String startDate; // ISO format: YYYY-MM-DD
    private String endDate;
    private String goal;
    private String status; // ACTIVE, CLOSED, FUTURE — optional, for updates only
}
