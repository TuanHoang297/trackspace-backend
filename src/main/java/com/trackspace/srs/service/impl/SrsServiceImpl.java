package com.trackspace.srs.service.impl;

import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.repository.JiraIssueRepository;
import com.trackspace.project.ProjectInfo;
import com.trackspace.project.ProjectInfoRepository;
import com.trackspace.srs.SrsDocument;
import com.trackspace.srs.SrsDocumentRepository;
import com.trackspace.srs.dto.SrsDocumentResponse;
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
        private final JiraIssueRepository jiraIssueRepository;
        private final SrsDocumentRepository srsDocumentRepository;
        private final UserRepository userRepository;
        private final AIPromptBuilder aiPromptBuilder;
        private final SrsExportService srsExportService;
        private final WebClient webClient;

        @Value("${ai.gemini.api-key}")
        private String geminiApiKey;

        @Value("${ai.gemini.model:gemini-1.5-flash}")
        private String geminiModel;

        private static final String SRS_NOT_FOUND = "Không tìm thấy SRS với ID: %d";

        // ==================== Generate ====================

        @Override
        @Transactional
        public SrsDocumentResponse generateSrs(Long projectId, Long currentUserId) {
                Objects.requireNonNull(projectId, "projectId cannot be null");
                Objects.requireNonNull(currentUserId, "currentUserId cannot be null");

                ProjectInfo info = projectInfoRepository.findByProjectId(projectId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project chưa có Project Info. Vui lòng điền thông tin dự án trước."));

                List<JiraIssue> issues = jiraIssueRepository.findByProjectId(projectId.intValue());

                User creator = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

                Integer maxVersion = srsDocumentRepository.findMaxVersionNumberByProjectId(projectId);
                Integer nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

                String groupName = info.getProject().getGroup().getGroupName();

                String promptText = aiPromptBuilder.buildPrompt(
                                info, issues, groupName, creator.getFullName(), nextVersion);

                String markdownContent = callGeminiApi(promptText, null);

                String title = "SRS - " + info.getProject().getProjectName() + " v" + nextVersion;

                // 10. Save and respond
                SrsDocument doc = SrsDocument.builder()
                                .project(info.getProject())
                                .versionNumber(nextVersion)
                                .title(title)
                                .content(markdownContent)
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

        private String callGeminiApi(String promptText, String pdfBase64) {
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
