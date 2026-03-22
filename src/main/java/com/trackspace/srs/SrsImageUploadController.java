package com.trackspace.srs;

import com.trackspace.common.ApiResponse;
import com.trackspace.srs.dto.ImageUploadResponse;
import com.trackspace.srs.service.impl.SrsImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/srs/images")
@Tag(name = "SRS Image", description = "Upload SRS images to Cloudinary")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SrsImageUploadController {

    private final SrsImageStorageService srsImageStorageService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Upload SRS image", description = "Upload image to Cloudinary and return a public URL")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = srsImageStorageService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", response));
    }
}
