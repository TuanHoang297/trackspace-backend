package com.trackspace.srs.service.impl;

import com.trackspace.srs.dto.ImageUploadResponse;
import com.trackspace.common.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SrsImageStorageService {

    private final WebClient webClient;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.folder:trackspace/srs}")
    private String cloudinaryFolder;

    @Value("${cloudinary.upload-preset:}")
    private String uploadPreset;

    public ImageUploadResponse uploadImage(MultipartFile file) {
        validateImage(file);
        String normalizedCloudName = normalizeCloudName(cloudName);
        String normalizedApiKey = normalizeValue(apiKey);
        String normalizedFolder = normalizeFolder(cloudinaryFolder);

        try {
            long timestamp = Instant.now().getEpochSecond();
            String signature = buildSignature(timestamp, normalizedFolder);
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + normalizedCloudName + "/image/upload";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("api_key", normalizedApiKey);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("signature", signature);
            if (!normalizedFolder.isBlank()) {
                body.add("folder", normalizedFolder);
            }
            body.add("file", asFileResource(file));

            Map<?, ?> result = webClient.post()
                    .uri(uploadUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (result == null) {
                throw new RuntimeException("Cloudinary không phản hồi dữ liệu");
            }

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            if (secureUrl == null || secureUrl.isBlank()) {
                throw new RuntimeException("Upload ảnh thất bại: Cloudinary không trả về URL");
            }

            return new ImageUploadResponse(secureUrl, publicId);
        } catch (WebClientResponseException e) {
            log.error("Cloudinary upload HTTP error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);

            if (e.getStatusCode().value() == 401) {
                String normalizedPreset = normalizeValue(uploadPreset);
                if (!normalizedPreset.isBlank()) {
                    return uploadUnsigned(file, normalizedCloudName, normalizedFolder, normalizedPreset);
                }

                String detail = e.getResponseBodyAsString();
                throw new BadRequestException("Cloudinary từ chối xác thực (401). " +
                        "Kiểm tra cloud-name/api-key/api-secret. Chi tiết: " + detail);
            }

            throw new BadRequestException("Cloudinary lỗi HTTP: " + e.getStatusCode().value()
                    + " - " + e.getResponseBodyAsString());
        } catch (IOException e) {
            log.error("Cloudinary upload IO error", e);
            throw new BadRequestException("Không thể đọc file ảnh để upload");
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("Upload ảnh lên Cloudinary thất bại");
        }
    }

    private ImageUploadResponse uploadUnsigned(
            MultipartFile file,
            String normalizedCloudName,
            String normalizedFolder,
            String normalizedPreset) {
        try {
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + normalizedCloudName + "/image/upload";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("upload_preset", normalizedPreset);
            if (!normalizedFolder.isBlank()) {
                body.add("folder", normalizedFolder);
            }
            body.add("file", asFileResource(file));

            Map<?, ?> result = webClient.post()
                    .uri(uploadUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (result == null) {
                throw new BadRequestException("Cloudinary không phản hồi dữ liệu (unsigned upload)");
            }

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new BadRequestException("Upload ảnh thất bại: Cloudinary không trả về URL (unsigned)");
            }

            log.info("Cloudinary unsigned upload fallback succeeded");
            return new ImageUploadResponse(secureUrl, publicId);
        } catch (WebClientResponseException e) {
            log.error("Cloudinary unsigned upload HTTP error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BadRequestException("Cloudinary unsigned upload thất bại: " + e.getResponseBodyAsString());
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc file ảnh để unsigned upload");
        }
    }

    private String buildSignature(long timestamp, String normalizedFolder) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("timestamp", String.valueOf(timestamp));
        if (!normalizedFolder.isBlank()) {
            params.put("folder", normalizedFolder);
        }

        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (raw.length() > 0) {
                raw.append('&');
            }
            raw.append(entry.getKey()).append('=').append(entry.getValue());
        }
        raw.append(normalizeValue(apiSecret));

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo Cloudinary signature", e);
        }
    }

    private ByteArrayResource asFileResource(MultipartFile file) throws IOException {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload-image";
            }
        };
    }

    private void validateImage(MultipartFile file) {
        if (normalizeCloudName(cloudName).isBlank() || normalizeValue(apiKey).isBlank() || normalizeValue(apiSecret).isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình Cloudinary (cloud-name/api-key/api-secret)");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file ảnh");
        }
    }

    private String normalizeCloudName(String value) {
        return normalizeValue(value).replaceAll("\\s+", "");
    }

    private String normalizeFolder(String value) {
        return normalizeValue(value);
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }
}
