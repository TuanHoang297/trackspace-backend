package com.trackspace.jira.dto;

import com.trackspace.jira.SprintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JiraSprintResponse {
    private Integer sprintId;
    private Integer projectId;
    private String jiraSprintId;
    private String sprintName;
    private String sprintGoal;
    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;
    private Long totalIssues;
    private Long doneIssues;
}
