package com.trackspace.srs;

import com.trackspace.common.ApiResponse;
import com.trackspace.project.ProjectInfo;
import com.trackspace.project.ProjectInfoRepository;
import com.trackspace.srs.dto.SrsVisionRequest;
import com.trackspace.srs.service.impl.SrsVisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for AI Vision — Analyze images and generate SRS section text.
 * Supports 4 image types: usecase, screenflow, db_schema, mockup.
 */
@RestController
@Tag(name = "SRS Vision", description = "AI Vision: Analyze diagrams/mockups and generate SRS descriptions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SrsVisionController {

    private final SrsVisionService srsVisionService;
    private final ProjectInfoRepository projectInfoRepository;

    @PostMapping("/api/projects/{projectId}/srs/describe-image")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Analyze image and generate SRS text",
            description = "Send a diagram/mockup image and receive structured SRS text. Types: usecase, screenflow, db_schema, mockup.")
    public ResponseEntity<ApiResponse<String>> describeImage(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody SrsVisionRequest request) {

        // Get project context for better AI results
        String projectContext = "";
        List<String> roles = List.of("Admin", "Lecturer", "Team Leader", "Team Member");

        ProjectInfo info = projectInfoRepository.findByProjectId(projectId).orElse(null);
        if (info != null) {
            projectContext = "Project: " + nullSafe(info.getTopic())
                    + ". Context: " + nullSafe(info.getContext())
                    + ". Actors: " + nullSafe(info.getPrimaryActors());

            // Parse roles from primary actors if available
            if (info.getPrimaryActors() != null && !info.getPrimaryActors().isBlank()) {
                roles = List.of(info.getPrimaryActors().split("[,|\\n]"));
                roles = roles.stream()
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
            }
        }

        String generatedText = srsVisionService.describeImage(request, projectContext, roles);

        return ResponseEntity.ok(ApiResponse.success("Phân tích ảnh thành công", generatedText));
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "";
    }
}
