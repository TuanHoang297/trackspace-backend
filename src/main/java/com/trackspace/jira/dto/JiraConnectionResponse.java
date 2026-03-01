package com.trackspace.jira.dto;

import com.trackspace.jira.JiraConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JiraConnectionResponse {
    private Integer connectionId;
    private Integer projectId;
    private String siteUrl;
    private String email;
    private String projectKey;
    private JiraConnectionStatus connectionStatus;
    private Instant lastSyncAt;
    private Long totalSprints;
    private Long totalIssues;
}
