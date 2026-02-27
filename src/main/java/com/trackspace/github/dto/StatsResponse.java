package com.trackspace.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsResponse {
    private Integer userId;
    private String userName;
    private Long totalCommits;
    private Long totalLinesAdded;
    private Long totalLinesDeleted;
    private Long totalChanges;
    private Instant lastCommitAt;
}
