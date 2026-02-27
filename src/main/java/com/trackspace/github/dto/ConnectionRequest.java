package com.trackspace.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionRequest {

    @NotNull(message = "Project ID is required")
    private Integer projectId;

    @NotBlank(message = "Repository URL is required")
    @Pattern(regexp = "https://github\\.com/[\\w-]+/[\\w-]+.*",
            message = "Invalid GitHub repository URL format")
    private String repositoryUrl;


    @NotBlank(message = "Access token is required")
    private String accessToken;

    private String branchName = "main";
}
