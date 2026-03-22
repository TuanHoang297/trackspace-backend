package com.trackspace.srs;

import com.trackspace.auth.AuthService;
import com.trackspace.common.ApiResponse;
import com.trackspace.srs.dto.DocxExportRequest;
import com.trackspace.srs.dto.SrsDocumentResponse;
import com.trackspace.srs.dto.SrsGenerateRequest;
import com.trackspace.srs.dto.SrsUpdateRequest;
import com.trackspace.srs.service.SrsService;
import com.trackspace.srs.service.impl.SrsExportService;
import com.trackspace.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller for AI-Powered SRS Generation and Management.
 */
@RestController
@Tag(name = "SRS", description = "AI-Powered SRS generation and document management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SrsController {

    private final SrsService srsService;
    private final AuthService authService;
    private final SrsExportService srsExportService;

    @PostMapping("/api/projects/{projectId}/srs/generate")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Generate SRS using AI", description = "Generates SRS draft with 4 basic sections from Jira issues + ProjectInfo + optional supplement data.")
    public ResponseEntity<ApiResponse<SrsDocumentResponse>> generateSrs(
            @PathVariable Long projectId,
            @RequestBody(required = false) SrsGenerateRequest request) {
        User currentUser = authService.getCurrentUser();
        SrsDocumentResponse response = srsService.generateSrs(projectId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo SRS thành công", response));
    }

    @GetMapping("/api/projects/{projectId}/srs")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @Operation(summary = "Get latest SRS version")
    public ResponseEntity<ApiResponse<SrsDocumentResponse>> getLatestSrs(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(srsService.getLatestSrs(projectId)));
    }

    @GetMapping("/api/projects/{projectId}/srs/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @Operation(summary = "Get all SRS versions")
    public ResponseEntity<ApiResponse<List<SrsDocumentResponse>>> getAllVersions(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(srsService.getAllVersions(projectId)));
    }

    @PutMapping("/api/srs/{srsId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @Operation(summary = "Update SRS content", description = "Creates a new SRS version with updated content.")
    public ResponseEntity<ApiResponse<SrsDocumentResponse>> updateSrs(
            @PathVariable Long srsId,
            @Valid @RequestBody SrsUpdateRequest request) {
        User currentUser = authService.getCurrentUser();
        SrsDocumentResponse response = srsService.updateSrs(srsId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật SRS thành công", response));
    }

    @PostMapping("/api/srs/export-doc")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @Operation(summary = "Export HTML content to DOC", description = "Receives HTML from the editor and returns a .doc file.")
    public ResponseEntity<byte[]> exportHtmlToDoc(@RequestBody DocxExportRequest request) {
        byte[] data = srsExportService.exportToDoc(request.getHtmlContent(), request.getTitle());

        String fileName = request.getFileName() != null ? request.getFileName() : "SRS_Export.doc";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/msword"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
