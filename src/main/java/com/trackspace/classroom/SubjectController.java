package com.trackspace.classroom;

import com.trackspace.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@Tag(name = "Subject", description = "APIs for subject management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Get active subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getActiveSubjects() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách môn học thành công",
                subjectService.getActiveSubjects()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all subjects including inactive")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getAllSubjects() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách môn học thành công",
                subjectService.getAllSubjects()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo môn học thành công", subjectService.createSubject(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            @PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật môn học thành công",
                subjectService.updateSubject(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete subject (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa môn học thành công", null));
    }
}
