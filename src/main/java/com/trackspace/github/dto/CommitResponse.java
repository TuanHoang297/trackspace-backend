package com.trackspace.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitResponse {
    private Integer commitId;
    private String commitSha;
    private String commitMessage;
    private String authorName;
    private String authorEmail;
    private String githubLogin;
    private Integer authorId;
    private Instant commitDate;
    private Integer filesChanged;
    private Integer linesAdded;
    private Integer linesDeleted;
    private String branchName;
    private Integer linkedIssueId;
    private String repoLabel;
}
