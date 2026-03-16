package com.trackspace.srs.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.entity.JiraSprint;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.jira.repository.JiraSprintRepository;
import com.trackspace.project.Project;
import com.trackspace.project.ProjectInfo;
import com.trackspace.project.ProjectInfoRepository;
import com.trackspace.project.ProjectRepository;
import com.trackspace.srs.SrsDocument;
import com.trackspace.srs.SrsDocumentRepository;
import com.trackspace.srs.dto.SrsDocumentResponse;
import com.trackspace.srs.dto.SrsGenerateRequest;
import com.trackspace.srs.dto.SrsUpdateRequest;
import com.trackspace.srs.service.SrsService;
import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SrsServiceImpl implements SrsService {

        private final ProjectInfoRepository projectInfoRepository;
        private final ProjectRepository projectRepository;
        private final JiraIssueRepository jiraIssueRepository;
        private final JiraSprintRepository jiraSprintRepository;
        private final SrsDocumentRepository srsDocumentRepository;
        private final UserRepository userRepository;
        private final AIPromptBuilder aiPromptBuilder;
        private final SrsExportService srsExportService;
        private final WebClient webClient;
        private final ObjectMapper objectMapper;

        @Value("${ai.gemini.api-key}")
        private String geminiApiKey;

        @Value("${ai.gemini.model:gemini-1.5-flash}")
        private String geminiModel;

        private static final String SRS_NOT_FOUND = "Không tìm thấy SRS với ID: %d";

        // ==================== Generate ====================

        @Override
        @Transactional
        public SrsDocumentResponse generateSrs(Long projectId, Long currentUserId, SrsGenerateRequest request) {
                Objects.requireNonNull(projectId, "projectId cannot be null");
                Objects.requireNonNull(currentUserId, "currentUserId cannot be null");

                // ProjectInfo is optional (supplementary)
                ProjectInfo info = projectInfoRepository.findByProjectId(projectId).orElse(null);

                // Project is required — load directly
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project không tồn tại với ID: " + projectId));

                List<JiraIssue> issues = jiraIssueRepository.findByProjectId(projectId.intValue());
                List<JiraSprint> sprints = jiraSprintRepository.findByProjectIdOrderByStartDateAsc(projectId.intValue());

                User creator = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

                Integer maxVersion = srsDocumentRepository.findMaxVersionNumberByProjectId(projectId);
                Integer nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

                String groupName = project.getGroup() != null ? project.getGroup().getGroupName() : "";
                String projectName = project.getProjectName();

                String promptText = aiPromptBuilder.buildPrompt(
                                info, issues, sprints, groupName, creator.getFullName(), nextVersion,
                                request != null ? request.getBusinessRules() : null,
                                request != null ? request.getNonScreenFunctions() : null,
                                request != null ? request.getNotes() : null);

                String jsonContent = callGeminiApiWithRetry(promptText);

                String title = "SRS - " + projectName + " v" + nextVersion;

                SrsDocument doc = SrsDocument.builder()
                                .project(project)
                                .versionNumber(nextVersion)
                                .title(title)
                                .content(jsonContent)
                                .createdBy(creator)
                                .build();

                SrsDocument savedDoc = srsDocumentRepository.save(doc);
                return toResponse(savedDoc);
        }

        // ==================== Read ====================

        @Override
        @Transactional(readOnly = true)
        public SrsDocumentResponse getLatestSrs(Long projectId) {
                Objects.requireNonNull(projectId, "projectId cannot be null");
                return srsDocumentRepository
                                .findFirstByProjectIdOrderByVersionNumberDesc(projectId)
                                .map(this::toResponse)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project chưa có SRS nào. Hãy generate SRS trước."));
        }

        @Override
        @Transactional(readOnly = true)
        public List<SrsDocumentResponse> getAllVersions(Long projectId) {
                Objects.requireNonNull(projectId, "projectId cannot be null");
                return srsDocumentRepository
                                .findByProjectIdOrderByVersionNumberDesc(projectId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        // ==================== Update ====================

        @Override
        @Transactional
        public SrsDocumentResponse updateSrs(Long srsId, SrsUpdateRequest request, Long currentUserId) {
                Objects.requireNonNull(srsId, "srsId cannot be null");
                Objects.requireNonNull(currentUserId, "currentUserId cannot be null");

                SrsDocument existing = srsDocumentRepository.findById(srsId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                String.format(SRS_NOT_FOUND, srsId)));

                User editor = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

                Integer maxVersion = srsDocumentRepository
                                .findMaxVersionNumberByProjectId(existing.getProject().getId());
                Integer nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

                String newTitle = request.getTitle() != null ? request.getTitle() : existing.getTitle();

                SrsDocument newVersion = SrsDocument.builder()
                                .project(existing.getProject())
                                .versionNumber(nextVersion)
                                .title(newTitle)
                                .content(request.getContent())
                                .createdBy(editor)
                                .build();

                SrsDocument savedDoc = srsDocumentRepository.save(newVersion);
                return toResponse(savedDoc);
        }

        // ==================== Export ====================

        @Override
        @Transactional(readOnly = true)
        public byte[] exportToPdf(Long srsId) {
                Objects.requireNonNull(srsId, "srsId cannot be null");
                SrsDocument doc = srsDocumentRepository.findById(srsId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                String.format(SRS_NOT_FOUND, srsId)));
                return srsExportService.exportToPdf(doc.getContent(), doc.getTitle());
        }

        @Override
        @Transactional(readOnly = true)
        public byte[] exportToDocx(Long srsId) {
                Objects.requireNonNull(srsId, "srsId cannot be null");
                SrsDocument doc = srsDocumentRepository.findById(srsId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                String.format(SRS_NOT_FOUND, srsId)));
                return srsExportService.exportToDocx(doc.getContent(), doc.getTitle());
        }

        // ==================== Gemini API ====================

        private static final int MAX_RETRY_ATTEMPTS = 3;

        /**
         * Calls Gemini and validates the JSON response, retrying up to
         * {@link #MAX_RETRY_ATTEMPTS} times on incomplete/invalid output.
         * Hard errors (rate-limit, bad model name) are rethrown immediately.
         */
        private String callGeminiApiWithRetry(String promptText) {
                for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                        try {
                                String rawJson = callGeminiApi(promptText);
                                log.info("[SRS] Attempt {}/{} — response len={}", attempt, MAX_RETRY_ATTEMPTS,
                                        rawJson != null ? rawJson.length() : 0);
                                validateSrsJson(rawJson);
                                if (attempt > 1) {
                                        log.info("[SRS] Retry attempt {}/{} succeeded.", attempt, MAX_RETRY_ATTEMPTS);
                                }
                                return rawJson;
                        } catch (RuntimeException e) {
                                String msg = e.getMessage() != null ? e.getMessage() : "";
                                // Non-retryable: rate-limit or misconfigured model
                                if (msg.contains("quá tải") || msg.contains("model AI")) {
                                        throw e;
                                }
                                log.warn("[SRS] Attempt {}/{} FAILED: {}",
                                        attempt, MAX_RETRY_ATTEMPTS, msg);
                        }
                }
                log.error("[SRS] All {} attempts failed to produce valid SRS JSON.", MAX_RETRY_ATTEMPTS);
                throw new RuntimeException(
                        "AI không thể tạo nội dung hợp lệ sau các lần thử. Vui lòng thử lại sau ít phút.");
        }

        private String callGeminiApi(String promptText) {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                                + geminiModel + ":generateContent?key=" + geminiApiKey;

                List<Map<String, Object>> parts = new ArrayList<>();
                parts.add(Map.of("text", Objects.requireNonNull(promptText, "promptText cannot be null")));

                Map<String, Object> generationConfig = Map.of(
                                "maxOutputTokens", 8192,
                                "temperature", 0.7,
                                "responseMimeType", "application/json");

                Map<String, Object> requestBody = Map.of(
                                "contents", List.of(Map.of("parts", parts)),
                                "generationConfig", generationConfig);

                log.info("Calling Gemini API (model={})", geminiModel);

                try {
                        Map<?, ?> response = webClient.post()
                                        .uri(url)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(requestBody)
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .block();

                        if (response == null) {
                                throw new RuntimeException("Gemini API call returned null response");
                        }

                        return parseGeminiResponse(response);

                } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
                        log.error("Gemini API Rate Limit Exceeded (429)", e);
                        throw new RuntimeException("Hệ thống AI đang quá tải (vượt quá giới hạn miễn phí). Vui lòng đợi 1 phút rồi thử lại.");
                } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
                        log.error("Gemini API Model Not Found (404)", e);
                        throw new RuntimeException("Không tìm thấy model AI: " + geminiModel + ". Vui lòng kiểm tra lại cấu hình tên model.");
                } catch (Exception e) {
                        log.error("Gemini API Error", e);
                        throw new RuntimeException("Lỗi kết nối AI: " + e.getMessage());
                }
        }

        private String parseGeminiResponse(Map<?, ?> response) {
                try {
                        List<?> candidates = (List<?>) response.get("candidates");
                        if (candidates == null || candidates.isEmpty())
                                throw new RuntimeException("No candidates in response");
                        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
                        List<?> parts = (List<?>) content.get("parts");
                        return (String) ((Map<?, ?>) parts.get(0)).get("text");
                } catch (Exception e) {
                        log.error("Failed to parse Gemini response: {}", response, e);
                        throw new RuntimeException("AI generation thất bại. Vui lòng thử lại sau.");
                }
        }

        // ==================== JSON Validation ====================

        private static final List<String> REQUIRED_SRS_KEYS = List.of(
                "projectName", "introduction");

        /**
         * Validates that the AI-generated string is well-formed JSON and contains
         * all top-level SRS fields. Throws {@link RuntimeException} with a
         * user-friendly message if the content is incomplete or unparseable —
         * preventing corrupted data from being saved to the database.
         */
        private void validateSrsJson(String rawJson) {
                JsonNode root;
                try {
                        root = objectMapper.readTree(rawJson);
                } catch (Exception e) {
                        String preview = rawJson != null
                                ? rawJson.substring(0, Math.min(300, rawJson.length()))
                                : "(null)";
                        log.error("[SRS] AI returned unparseable JSON. Preview: {}...", preview);
                        throw new RuntimeException(
                                "AI trả về nội dung không hợp lệ (JSON bị lỗi cú pháp). Vui lòng thử lại.");
                }

                // 2. Root must be a JSON object
                if (!root.isObject()) {
                        log.error("[SRS] AI returned non-object JSON type: {}", root.getNodeType());
                        throw new RuntimeException(
                                "AI trả về nội dung không hợp lệ (không phải JSON object). Vui lòng thử lại.");
                }

                // 3. All required top-level fields must be present and non-null
                List<String> missing = REQUIRED_SRS_KEYS.stream()
                                .filter(key -> !root.has(key) || root.get(key).isNull())
                                .toList();

                if (!missing.isEmpty()) {
                        String preview = rawJson.substring(0, Math.min(300, rawJson.length()));
                        log.error("[SRS] AI returned JSON missing required fields: {}. Preview: {}...",
                                missing, preview);
                        throw new RuntimeException(
                                "AI trả về nội dung không đầy đủ (thiếu: " + String.join(", ", missing)
                                        + "). Nội dung có thể bị cắt ngắn. Vui lòng thử lại.");
                }
        }

        // ==================== Helpers ====================

        private SrsDocumentResponse toResponse(SrsDocument doc) {
                Objects.requireNonNull(doc, "doc cannot be null");
                return SrsDocumentResponse.builder()
                                .id(doc.getId())
                                .versionNumber(doc.getVersionNumber())
                                .title(doc.getTitle())
                                .content(doc.getContent())
                                .projectId(doc.getProject().getId())
                                .createdByName(doc.getCreatedBy().getFullName())
                                .updatedAt(doc.getUpdatedAt())
                                .build();
        }
}
