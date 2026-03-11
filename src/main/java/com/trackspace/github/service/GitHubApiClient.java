package com.trackspace.github.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client for GitHub REST API
 * Handles all external API calls to GitHub
 * Uses RestTemplate for synchronous/blocking HTTP calls
 */
@Service
@Slf4j
public class GitHubApiClient {

    private final RestTemplate restTemplate;
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";

    public GitHubApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .rootUri(GITHUB_API_BASE_URL)
                .build();
    }

    /**
     * Validate connection to GitHub repository
     * 
     * @param owner GitHub repository owner
     * @param repo  GitHub repository name
     * @param token GitHub personal access token
     * @return true if token is valid and has access to repository
     */
    public boolean validateConnection(String owner, String repo, String token) {
        log.info("Validating GitHub connection for {}/{}", owner, repo);

        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubRepoDto> response = restTemplate.exchange(
                    "/repos/{owner}/{repo}",
                    HttpMethod.GET,
                    entity,
                    GitHubRepoDto.class,
                    owner, repo);

            log.info("Successfully validated connection to {}/{}", owner, repo);
            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException ex) {
            log.error("Failed to validate GitHub connection: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Fetch commits from GitHub repository
     * 
     * @param owner  GitHub repository owner
     * @param repo   GitHub repository name
     * @param token  GitHub personal access token
     * @param since  Only commits after this timestamp (optional)
     * @param branch Branch name (optional, default: default branch)
     * @return List of commits
     */
    public List<GitHubCommitDto> fetchCommits(String owner, String repo, String token,
            Instant since, String branch) {
        log.info("Fetching commits from {}/{}, branch: {}, since: {}", owner, repo, branch, since);

        try {
            // Build URI with query parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromPath("/repos/{owner}/{repo}/commits")
                    .queryParam("per_page", 100);

            if (since != null) {
                uriBuilder.queryParam("since", since.toString());
            }
            if (branch != null && !branch.isEmpty()) {
                uriBuilder.queryParam("sha", branch);
            }

            // Dùng toUriString() để rootUri được áp dụng (không dùng toUri())
            String url = uriBuilder.buildAndExpand(owner, repo).toUriString();
            log.info(">>> Calling GitHub API URL: {}", url);
            log.info(">>> Token prefix: {}", token != null && token.length() > 10
                    ? token.substring(0, 10) + "..."
                    : "(empty or null)");

            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubCommitDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubCommitDto[].class);

            log.info(">>> GitHub API response status: {}", response.getStatusCode());

            List<GitHubCommitDto> commits = response.getBody() != null
                    ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            log.info("Completed fetching {} commits from {}/{}", commits.size(), owner, repo);
            return commits;

        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            // 409 Conflict → empty repository (no commits yet) → return empty
            if (ex.getStatusCode().value() == 409) {
                log.info("Repository {}/{} is empty (409 Conflict), returning empty commits", owner, repo);
                return Collections.emptyList();
            }
            // 401 Unauthorized hoặc 404 Not Found → ném lỗi rõ ràng
            String msg = switch (ex.getStatusCode().value()) {
                case 401 -> "GitHub token không hợp lệ hoặc đã hết hạn";
                case 403 -> "GitHub token không có quyền truy cập repo này";
                case 404 ->
                    "Không tìm thấy repo hoặc branch '" + branch + "' không tồn tại trong " + owner + "/" + repo;
                default -> "GitHub API lỗi: " + ex.getStatusCode().value() + " " + ex.getStatusText();
            };
            log.error(">>> GitHub API ERROR [{}]: {}", ex.getStatusCode(), msg);
            throw new com.trackspace.common.BadRequestException(msg);
        } catch (RestClientException ex) {
            log.error(">>> GitHub API ERROR fetching commits from {}/{}: [{}] {}",
                    owner, repo, ex.getClass().getSimpleName(), ex.getMessage());
            throw new com.trackspace.common.BadRequestException("Không thể kết nối GitHub API: " + ex.getMessage());
        }
    }

    /**
     * Fetch detailed information for a specific commit
     * Including file changes and statistics
     * 
     * @param owner GitHub repository owner
     * @param repo  GitHub repository name
     * @param sha   Commit SHA
     * @param token GitHub personal access token
     * @return Commit details or null if error
     */
    public GitHubCommitDetailDto fetchCommitDetails(String owner, String repo,
            String sha, String token) {
        log.debug("Fetching commit details for {}/{} - {}", owner, repo, sha);

        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubCommitDetailDto> response = restTemplate.exchange(
                    "/repos/{owner}/{repo}/commits/{sha}",
                    HttpMethod.GET,
                    entity,
                    GitHubCommitDetailDto.class,
                    owner, repo, sha);

            return response.getBody();

        } catch (RestClientException ex) {
            log.error("Error fetching commit details: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Get repository information
     * 
     * @param owner GitHub repository owner
     * @param repo  GitHub repository name
     * @param token GitHub personal access token
     * @return Repository information or null if error
     */
    public GitHubRepoDto getRepository(String owner, String repo, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubRepoDto> response = restTemplate.exchange(
                    "/repos/{owner}/{repo}",
                    HttpMethod.GET,
                    entity,
                    GitHubRepoDto.class,
                    owner, repo);

            return response.getBody();

        } catch (RestClientException ex) {
            log.error("Error fetching repository info: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Fetch all branches from a GitHub repository
     *
     * @param owner GitHub repository owner
     * @param repo  GitHub repository name
     * @param token GitHub personal access token
     * @return List of branches
     */
    public List<GitHubBranchDto> fetchBranches(String owner, String repo, String token) {
        log.info("Fetching branches from {}/{}", owner, repo);

        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = UriComponentsBuilder
                    .fromPath("/repos/{owner}/{repo}/branches")
                    .queryParam("per_page", 100)
                    .buildAndExpand(owner, repo)
                    .toUriString();

            ResponseEntity<GitHubBranchDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubBranchDto[].class);

            List<GitHubBranchDto> branches = response.getBody() != null
                    ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            log.info("Fetched {} branches from {}/{}", branches.size(), owner, repo);
            return branches;

        } catch (RestClientException ex) {
            log.error("Error fetching branches from {}/{}: {}", owner, repo, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Create HTTP headers for GitHub API requests
     * 
     * @param token GitHub personal access token
     * @return HttpHeaders with authorization and accept headers
     */
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);
        headers.set("Accept", "application/vnd.github+json");
        return headers;
    }

    // ============== DTOs for GitHub API responses ==============

    @Data
    public static class GitHubCommitDto {
        private String sha;
        private CommitInfo commit;
        private Author author;      // top-level GitHub user (author)
        private Author committer;   // top-level GitHub user (committer) — fallback when author is null
        private Stats stats;

        @Data
        public static class CommitInfo {
            private String message;
            private Author author;
            private Committer committer;
        }

        @Data
        public static class Author {
            private String name;
            private String email;
            private String date;
            private String login;
        }

        @Data
        public static class Committer {
            private String name;
            private String email;
            private String date;
        }

        @Data
        public static class Stats {
            private Integer total;
            private Integer additions;
            private Integer deletions;
        }
    }

    @Data
    public static class GitHubCommitDetailDto {
        private String sha;
        private CommitInfo commit;
        private Stats stats;
        private List<FileChange> files;

        @Data
        public static class CommitInfo {
            private String message;
            private Author author;
        }

        @Data
        public static class Author {
            private String name;
            private String email;
            private String date;
        }

        @Data
        public static class Stats {
            private Integer total;
            private Integer additions;
            private Integer deletions;
        }

        @Data
        public static class FileChange {
            private String filename;
            private String status;
            private Integer additions;
            private Integer deletions;
            private Integer changes;
            private String patch;
        }
    }

    @Data
    public static class GitHubRepoDto {
        private Long id;
        private String name;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("default_branch")
        private String defaultBranch;

        private Owner owner;

        @Data
        public static class Owner {
            private String login;
        }
    }

    @Data
    public static class GitHubBranchDto {
        private String name;

        @JsonProperty("protected")
        private Boolean isProtected;

        private BranchCommit commit;

        @Data
        public static class BranchCommit {
            private String sha;
            private String url;
        }
    }
}
