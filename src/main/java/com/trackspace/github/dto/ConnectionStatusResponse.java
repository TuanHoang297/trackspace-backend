package com.trackspace.github.dto;

import com.trackspace.github.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectionStatusResponse {
    private Integer connectionId;
    private Integer projectId;
    private String repositoryUrl;
    private String branchName;
    private ConnectionStatus connectionStatus;
    private Instant lastSyncAt;
    private Long totalCommits;
}
