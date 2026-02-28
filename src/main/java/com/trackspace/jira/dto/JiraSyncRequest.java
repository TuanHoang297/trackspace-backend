package com.trackspace.jira.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraSyncRequest {
    @NotNull(message = "Project ID is required")
    private Integer projectId;
}
