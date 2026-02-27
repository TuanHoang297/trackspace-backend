package com.trackspace.github.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest {
    @NotNull(message = "Project ID is required")
    private Integer projectId;

    private Instant since; // Optional: sync commits after this date

    private String branch;
}
