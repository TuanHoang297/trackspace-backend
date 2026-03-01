package com.trackspace.classroom;

import com.trackspace.auth.AuthService;
import com.trackspace.common.ApiResponse;
import com.trackspace.user.User;
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

/**
 * Class Controller
 * REST endpoints for classroom management (CRUD + student enrollment)
 */
@RestController
@RequestMapping("/api/classes")
@Tag(name = "Classroom", description = "APIs for classroom management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ClassController {

        private final ClassService classService;
        private final AuthService authService;

        // ==================== Class CRUD ====================

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Create class", description = "Admin creates a new class. Optionally include lecturerId to assign a lecturer immediately.")
        public ResponseEntity<ApiResponse<ClassResponse>> createClass(
                        @Valid @RequestBody CreateClassRequest request) {
                ClassResponse response = classService.createClass(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.success("Tạo lớp học thành công", response));
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
        @Operation(summary = "Get all classes", description = "Admin gets all classes; Lecturer gets their own classes")
        public ResponseEntity<ApiResponse<List<ClassResponse>>> getAllClasses() {
                User currentUser = authService.getCurrentUser();
                List<ClassResponse> classes = classService.getAllClasses(currentUser);
                return ResponseEntity.ok(
                                ApiResponse.success("Lấy danh sách lớp học thành công", classes));
        }

        @GetMapping("/{classId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
        @Operation(summary = "Get class by ID", description = "Get detailed information of a specific class")
        public ResponseEntity<ApiResponse<ClassResponse>> getClassById(@PathVariable Long classId) {
                ClassResponse response = classService.getClassById(classId);
                return ResponseEntity.ok(
                                ApiResponse.success("Lấy thông tin lớp học thành công", response));
        }

        @PutMapping("/{classId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Update class", description = "Admin updates class name, semester, active status, or lecturer assignment")
        public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
                        @PathVariable Long classId,
                        @Valid @RequestBody UpdateClassRequest request) {
                ClassResponse response = classService.updateClass(classId, request);
                return ResponseEntity.ok(
                                ApiResponse.success("Cập nhật lớp học thành công", response));
        }

        @PutMapping("/{classId}/lecturer")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Assign lecturer to class", description = "Admin assigns or changes the lecturer for a class")
        public ResponseEntity<ApiResponse<ClassResponse>> assignLecturer(
                        @PathVariable Long classId,
                        @RequestBody AssignLecturerRequest request) {
                ClassResponse response = classService.assignLecturer(classId, request.getLecturerId());
                return ResponseEntity.ok(
                                ApiResponse.success("Gán giảng viên thành công", response));
        }

        @DeleteMapping("/{classId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Delete class", description = "Admin soft-deletes a class (sets active = false)")
        public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable Long classId) {
                classService.deleteClass(classId);
                return ResponseEntity.ok(
                                ApiResponse.success("Xóa lớp học thành công", null));
        }

        // ==================== Student Enrollment ====================

        @GetMapping("/{classId}/students")
        @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
        @Operation(summary = "Get students in class", description = "Get list of students enrolled in a specific class")
        public ResponseEntity<ApiResponse<List<StudentInClassResponse>>> getStudentsByClass(
                        @PathVariable Long classId) {
                List<StudentInClassResponse> students = classService.getStudentsByClassId(classId);
                return ResponseEntity.ok(
                                ApiResponse.success("Lấy danh sách sinh viên thành công", students));
        }

        @PostMapping("/{classId}/students")
        @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
        @Operation(summary = "Add student to class", description = "Add a student (TEAMLEADER or TEAMMEMBER) to a class")
        public ResponseEntity<ApiResponse<StudentInClassResponse>> addStudentToClass(
                        @PathVariable Long classId,
                        @Valid @RequestBody AddStudentRequest request) {
                StudentInClassResponse response = classService.addStudentToClass(classId, request.getStudentId());
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.success("Thêm sinh viên vào lớp thành công", response));
        }

        @DeleteMapping("/{classId}/students/{studentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
        @Operation(summary = "Remove student from class", description = "Remove a student from a class")
        public ResponseEntity<ApiResponse<Void>> removeStudentFromClass(
                        @PathVariable Long classId,
                        @PathVariable Long studentId) {
                classService.removeStudentFromClass(classId, studentId);
                return ResponseEntity.ok(
                                ApiResponse.success("Xóa sinh viên khỏi lớp thành công", null));
        }
}
