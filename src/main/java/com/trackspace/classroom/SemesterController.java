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
@RequestMapping("/api/semesters")
@Tag(name = "Semester", description = "APIs for semester management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Get all active semesters")
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> getAllSemesters() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách học kỳ thành công",
                semesterService.getAllActiveSemesters()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all semesters including inactive (Admin only)")
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> getAllSemestersAdmin() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách học kỳ thành công",
                semesterService.getAllSemesters()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create semester (Admin only)")
    public ResponseEntity<ApiResponse<SemesterResponse>> createSemester(@Valid @RequestBody SemesterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Tạo học kỳ thành công", semesterService.createSemester(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update semester (Admin only)")
    public ResponseEntity<ApiResponse<SemesterResponse>> updateSemester(
            @PathVariable("id") Long id, @Valid @RequestBody SemesterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật học kỳ thành công",
                semesterService.updateSemester(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete semester (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteSemester(@PathVariable("id") Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa học kỳ thành công", null));
    }
}
