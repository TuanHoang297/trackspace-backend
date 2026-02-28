package com.trackspace.jira.entity;

import com.trackspace.jira.SprintStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "jira_sprints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraSprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sprint_id", nullable = false)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Size(max = 100)
    @NotNull
    @Column(name = "jira_sprint_id", nullable = false, unique = true, length = 100)
    private String jiraSprintId;

    @Size(max = 255)
    @NotNull
    @Column(name = "sprint_name", nullable = false)
    private String sprintName;

    @Lob
    @Column(name = "sprint_goal")
    private String sprintGoal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SprintStatus status;

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
