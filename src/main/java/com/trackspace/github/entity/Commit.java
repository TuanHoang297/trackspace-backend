package com.trackspace.github.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "github_commits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"commit_sha", "connection_id"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Commit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commit_id", nullable = false)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "connection_id")
    private Integer connectionId;

    @Size(max = 100)
    @NotNull
    @Column(name = "commit_sha", nullable = false, length = 100)
    private String commitSha;

    @NotNull
    @Column(name = "commit_message", nullable = false, columnDefinition = "TEXT")
    private String commitMessage;

    @Size(max = 255)
    @NotNull
    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Size(max = 255)
    @Column(name = "author_email")
    private String authorEmail;

    @Size(max = 100)
    @Column(name = "github_login", length = 100)
    private String githubLogin;

    @Column(name = "author_id")
    private Integer authorId;

    @NotNull
    @Column(name = "commit_date", nullable = false)
    private Instant commitDate;

    @Column(name = "files_changed", columnDefinition = "int UNSIGNED")
    private Integer filesChanged;

    @Column(name = "lines_added", columnDefinition = "int UNSIGNED")
    private Integer linesAdded;

    @Column(name = "lines_deleted", columnDefinition = "int UNSIGNED")
    private Integer linesDeleted;

    /** Lines added excluding library/generated files (for contribution analytics only) */
    @Column(name = "lines_added_code", columnDefinition = "int UNSIGNED")
    private Integer linesAddedCode;

    /** Lines deleted excluding library/generated files (for contribution analytics only) */
    @Column(name = "lines_deleted_code", columnDefinition = "int UNSIGNED")
    private Integer linesDeletedCode;

    /** Per-file weighted lines: Σ(file.additions × fileWeight). Used by V2 contribution formula. */
    @Column(name = "weighted_lines_added")
    private Double weightedLinesAdded;

    @Column(name = "branch_name", columnDefinition = "TEXT")
    private String branchName;

    @Column(name = "linked_issue_id")
    private Integer linkedIssueId;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
