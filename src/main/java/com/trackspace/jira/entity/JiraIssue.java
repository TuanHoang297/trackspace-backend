package com.trackspace.jira.entity;

import com.trackspace.jira.IssueType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "jira_issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id", nullable = false)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "sprint_id")
    private Integer sprintId;

    @Size(max = 100)
    @NotNull
    @Column(name = "jira_issue_id", nullable = false, unique = true, length = 100)
    private String jiraIssueId;

    @Size(max = 50)
    @NotNull
    @Column(name = "issue_key", nullable = false, length = 50)
    private String issueKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType;

    @Size(max = 500)
    @NotNull
    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Lob
    @Column(name = "description")
    private String description;

    @Size(max = 50)
    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Size(max = 50)
    @Column(name = "priority", length = 50)
    private String priority;

    @Column(name = "assignee_id")
    private Integer assigneeId;

    @Size(max = 255)
    @Column(name = "assignee_name")
    private String assigneeName;

    @Size(max = 255)
    @Column(name = "assignee_email")
    private String assigneeEmail;

    @Size(max = 255)
    @Column(name = "jira_account_id")
    private String jiraAccountId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
