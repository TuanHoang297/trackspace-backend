package com.trackspace.jira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraConnectionRequest {

    @NotNull(message = "Project ID is required")
    private Integer projectId;

    @NotBlank(message = "Jira site URL is required")
    private String siteUrl;

    @NotBlank(message = "Jira email is required")
    private String email;

    @NotBlank(message = "API token is required")
    private String apiToken;

    @NotBlank(message = "Jira project key is required")
    private String projectKey;
}
