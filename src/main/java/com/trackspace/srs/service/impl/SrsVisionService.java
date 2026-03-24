package com.trackspace.srs.service.impl;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ServiceUnavailableException;
import com.trackspace.srs.dto.SrsVisionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    @Value("${ai.gemini.model:gemini-3.1-flash-lite-preview}")
    private String geminiModel;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MAX_DOWNLOAD_BYTES = 10 * 1024 * 1024;

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
            } catch (BadRequestException e) {
                // Validation / payload errors are not transient, so do not retry.
                throw e;
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("quá tải") || msg.contains("model AI")) {
                    throw e;
                }
                log.warn("[SRS Vision] Attempt {}/{} failed: {}. Retrying...",
                        attempt, MAX_RETRY_ATTEMPTS, msg);
            }
        }
        throw new ServiceUnavailableException(
                "Hệ thống AI (Gemini) hiện đang bận hoặc quá tải sau nhiều lần thử. Vui lòng quay lại thử lại sau ít phút.");
    }

    private String callGeminiVision(String prompt, String imageInput) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        // Accept both base64 data URL and HTTP image URL (e.g. Cloudinary)
        String cleanBase64;
        String mimeType;
        if (isHttpUrl(imageInput)) {
            ImagePayload payload = downloadImageAsBase64(imageInput);
            cleanBase64 = payload.base64();
            mimeType = payload.mimeType();
        } else {
            ImagePayload payload = parseBase64Image(imageInput);
            cleanBase64 = payload.base64();
            mimeType = payload.mimeType();
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
                throw new BadRequestException("Gemini Vision API trả về phản hồi rỗng");
            }

            return parseGeminiResponse(response);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            log.error("Gemini Vision API Rate Limit (429)", e);
            throw new ServiceUnavailableException("Hệ thống AI đang quá tải (Rate Limit). Vui lòng đợi 1 phút rồi thử lại.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            log.error("Gemini Vision API Model Not Found (404)", e);
            throw new BadRequestException("Không tìm thấy model AI: " + geminiModel + ". Vui lòng kiểm tra cấu hình ai.gemini.model.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest e) {
            log.error("Gemini Vision API Bad Request (400): {}", e.getResponseBodyAsString(), e);
            throw new BadRequestException("Yêu cầu AI Vision không hợp lệ: " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini Vision API Error", e);
            throw new ServiceUnavailableException("Không thể kết nối được tới AI Vision: " + e.getMessage());
        }
    }

    private String parseGeminiResponse(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty())
                throw new BadRequestException("AI Vision không trả về candidates hợp lệ");
            Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            List<?> parts = (List<?>) content.get("parts");
            return (String) ((Map<?, ?>) parts.get(0)).get("text");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini Vision response: {}", response, e);
            throw new BadRequestException("AI Vision trả dữ liệu không đúng định dạng");
        }
    }

    private boolean isHttpUrl(String value) {
        String normalized = value != null ? value.trim().toLowerCase() : "";
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private ImagePayload parseBase64Image(String imageBase64) {
        String cleanBase64 = imageBase64;
        String mimeType = "image/png";

        if (imageBase64.contains(",")) {
            String[] parts = imageBase64.split(",", 2);
            cleanBase64 = parts[1];
            if (parts[0].contains("image/jpeg") || parts[0].contains("image/jpg")) {
                mimeType = "image/jpeg";
            } else if (parts[0].contains("image/webp")) {
                mimeType = "image/webp";
            }
        }

        try {
            Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Dữ liệu ảnh base64 không hợp lệ");
        }

        return new ImagePayload(cleanBase64, mimeType);
    }

    private ImagePayload downloadImageAsBase64(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new BadRequestException("Không thể tải ảnh từ URL để phân tích. HTTP " + status);
            }

            String mimeType = connection.getContentType() != null
                    ? connection.getContentType()
                    : "image/png";
            if (!mimeType.toLowerCase().startsWith("image/")) {
                throw new BadRequestException("URL không trỏ tới file ảnh hợp lệ");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            int total = 0;
            byte[] bytes;

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    total += bytesRead;
                    if (total > MAX_DOWNLOAD_BYTES) {
                        throw new BadRequestException("Ảnh quá lớn để phân tích (tối đa 10MB)");
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
                bytes = outputStream.toByteArray();
            }

            if (bytes.length == 0) {
                throw new BadRequestException("Image URL does not contain valid data");
            }

            return new ImagePayload(Base64.getEncoder().encodeToString(bytes), mimeType);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download image from URL: {}", imageUrl, e);
            throw new BadRequestException("Không thể tải ảnh từ URL để phân tích");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private record ImagePayload(String base64, String mimeType) {
    }
}
