package com.trackspace.srs.service.impl;

import com.trackspace.srs.dto.SrsVisionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SrsVisionService {

    private final AIPromptBuilder aiPromptBuilder;
    private final WebClient webClient;

    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Analyzes an image and generates SRS text based on the image type.
     * Supports: usecase, screenflow, db_schema, mockup
     */
    public String describeImage(SrsVisionRequest request, String projectContext, List<String> roles) {
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(request.getImage(), "image cannot be null");
        Objects.requireNonNull(request.getType(), "type cannot be null");

        String validatedType = validateImageType(request.getType());

        String prompt = aiPromptBuilder.buildVisionPrompt(
                validatedType,
                projectContext != null ? projectContext : request.getContext(),
                roles);

        return callGeminiVisionWithRetry(prompt, request.getImage());
    }

    private String validateImageType(String type) {
        return switch (type.toLowerCase().trim()) {
            case "usecase" -> "usecase";
            case "screenflow" -> "screenflow";
            case "db_schema" -> "db_schema";
            case "mockup" -> "mockup";
            default -> throw new IllegalArgumentException(
                    "Invalid image type: " + type + ". Must be one of: usecase, screenflow, db_schema, mockup");
        };
    }

    private String callGeminiVisionWithRetry(String prompt, String imageBase64) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                String result = callGeminiVision(prompt, imageBase64);
                if (attempt > 1) {
                    log.info("[SRS Vision] Retry attempt {}/{} succeeded.", attempt, MAX_RETRY_ATTEMPTS);
                }
                return result;
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("quá tải") || msg.contains("model AI")) {
                    throw e;
                }
                log.warn("[SRS Vision] Attempt {}/{} failed: {}. Retrying...",
                        attempt, MAX_RETRY_ATTEMPTS, msg);
            }
        }
        throw new RuntimeException(
                "AI không thể phân tích ảnh sau các lần thử. Vui lòng thử lại sau.");
    }

    private String callGeminiVision(String prompt, String imageBase64) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        // Clean base64 string — remove data URL prefix if present
        String cleanBase64 = imageBase64;
        String mimeType = "image/png";
        if (imageBase64.contains(",")) {
            String[] parts = imageBase64.split(",", 2);
            cleanBase64 = parts[1];
            // Extract MIME type from data URL (e.g. data:image/jpeg;base64,)
            if (parts[0].contains("image/jpeg") || parts[0].contains("image/jpg")) {
                mimeType = "image/jpeg";
            } else if (parts[0].contains("image/webp")) {
                mimeType = "image/webp";
            }
        }

        // Validate base64
        try {
            Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid base64 image data");
        }

        // Build multimodal request: text prompt + image
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        parts.add(Map.of("inlineData", Map.of(
                "mimeType", mimeType,
                "data", cleanBase64)));

        Map<String, Object> generationConfig = Map.of(
                "maxOutputTokens", 4096,
                "temperature", 0.3,
                "responseMimeType", "application/json");

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", generationConfig);

        log.info("[SRS Vision] Calling Gemini Vision API (model={}, type=image)", geminiModel);

        try {
            Map<?, ?> response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Gemini Vision API returned null response");
            }

            return parseGeminiResponse(response);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            log.error("Gemini Vision API Rate Limit (429)", e);
            throw new RuntimeException("AI đang quá tải. Vui lòng đợi 1 phút rồi thử lại.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            log.error("Gemini Vision API Model Not Found (404)", e);
            throw new RuntimeException("Không tìm thấy model AI: " + geminiModel);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini Vision API Error", e);
            throw new RuntimeException("Lỗi kết nối AI Vision: " + e.getMessage());
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
            log.error("Failed to parse Gemini Vision response: {}", response, e);
            throw new RuntimeException("AI Vision thất bại. Vui lòng thử lại.");
        }
    }
}
