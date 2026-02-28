package com.trackspace.jira.dto;

import com.trackspace.jira.IssueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JiraIssueResponse {
    private Integer issueId;
    private Integer projectId;
    private Integer sprintId;
    private String jiraIssueId;
    private String issueKey;
    private IssueType issueType;
    private String summary;
    private String description;
    private String status;
    private String priority;
    private Integer assigneeId;
    private String assigneeName;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
}
