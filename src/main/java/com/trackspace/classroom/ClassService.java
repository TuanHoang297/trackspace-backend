package com.trackspace.classroom;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Class Service
 * Business logic for classroom management
 */
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;

    private static final String CLASS_NOT_FOUND = "Không tìm thấy lớp học với ID: %d";
    private static final String USER_NOT_FOUND = "Không tìm thấy người dùng với ID: %d";

    // ==================== Class CRUD ====================

    /**
     * Create a new class (Admin only)
     *
     * @param request Create class request
     * @return Created class response
     */
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        if (classRepository.existsByClassCode(request.getClassCode())) {
            throw new BadRequestException("Mã lớp '" + request.getClassCode() + "' đã tồn tại");
        }
        Class newClass = Class.builder()
                .className(request.getClassName())
                .classCode(request.getClassCode())
                .semester(request.getSemester())
                .active(true)
                .build();
        Class saved = classRepository.save(newClass);
        return buildClassResponse(saved, 0L);
    }

    /**
     * Assign or change lecturer for a class (Admin only)
     *
     * @param classId    Class ID
     * @param lecturerId Lecturer user ID
     * @return Updated class response
     */
    @Transactional
    public ClassResponse assignLecturer(Long classId, Long lecturerId) {
        Class aClass = findActiveClassById(classId);
        aClass.setLecturer(findLecturerById(lecturerId));
        Class updated = classRepository.save(aClass);
        long studentCount = classStudentRepository.countByClassroomId(updated.getId());
        return buildClassResponse(updated, studentCount);
    }

    /**
     * Get all active classes (Admin sees all, Lecturer sees their own)
     *
     * @param currentUser Authenticated user
     * @return List of class responses
     */
    @Transactional(readOnly = true)
    public List<ClassResponse> getAllClasses(User currentUser) {
        List<Class> classes;

        if (currentUser.getRole() == User.Role.ADMIN) {
            classes = classRepository.findByActiveTrue();
        } else {
            classes = classRepository.findByLecturerIdAndActiveTrue(currentUser.getId());
        }

        return classes.stream()
                .map(c -> buildClassResponse(c, classStudentRepository.countByClassroomId(c.getId())))
                .toList();
    }

    /**
     * Get class by ID
     *
     * @param classId Class ID
     * @return Class response
     */
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long classId) {
        Class aClass = findActiveClassById(classId);
        long studentCount = classStudentRepository.countByClassroomId(classId);
        return buildClassResponse(aClass, studentCount);
    }

    /**
     * Update class details (Admin only)
     *
     * @param classId Class ID
     * @param request Update class request
     * @return Updated class response
     */
    @Transactional
    public ClassResponse updateClass(Long classId, UpdateClassRequest request) {
        Class aClass = findActiveClassById(classId);
        applyUpdates(aClass, request);
        Class updated = classRepository.save(aClass);
        long studentCount = classStudentRepository.countByClassroomId(updated.getId());
        return buildClassResponse(updated, studentCount);
    }

    private void applyUpdates(Class aClass, UpdateClassRequest request) {
        if (request.getClassName() != null && !request.getClassName().isBlank()) {
            aClass.setClassName(request.getClassName());
        }
        if (request.getSemester() != null && !request.getSemester().isBlank()) {
            aClass.setSemester(request.getSemester());
        }
        if (request.getActive() != null) {
            aClass.setActive(request.getActive());
        }
    }

    /**
     * Delete (soft-delete) a class (Admin only)
     *
     * @param classId Class ID
     */
    @Transactional
    public void deleteClass(Long classId) {
        Class aClass = findActiveClassById(classId);
        aClass.setActive(false);
        classRepository.save(aClass);
    }

    // ==================== Student Management ====================

    /**
     * Get all students enrolled in a class
     *
     * @param classId Class ID
     * @return List of student responses
     */
    @Transactional(readOnly = true)
    public List<StudentInClassResponse> getStudentsByClassId(Long classId) {
        findActiveClassById(classId);
        return classStudentRepository.findByClassIdWithStudent(classId).stream()
                .map(this::buildStudentInClassResponse)
                .toList();
    }

    /**
     * Add a student to a class
     *
     * @param classId   Class ID
     * @param studentId Student user ID
     * @return Added student response
     */
    @Transactional
    public StudentInClassResponse addStudentToClass(Long classId, Long studentId) {
        Class aClass = findActiveClassById(classId);
        User student = findStudentById(studentId);

        if (classStudentRepository.existsByClassroomIdAndStudentId(classId, studentId)) {
            throw new BadRequestException("Sinh viên đã được thêm vào lớp này");
        }

        ClassStudent classStudent = ClassStudent.builder()
                .classroom(aClass)
                .student(student)
                .build();

        ClassStudent saved = classStudentRepository.save(classStudent);
        return buildStudentInClassResponse(saved);
    }

    /**
     * Remove a student from a class
     *
     * @param classId   Class ID
     * @param studentId Student user ID
     */
    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        findActiveClassById(classId);
        ClassStudent classStudent = classStudentRepository
                .findByClassroomIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sinh viên không thuộc lớp học này"));
        classStudentRepository.delete(classStudent);
    }

    // ==================== Helper Methods ====================

    private Class findActiveClassById(Long classId) {
        return classRepository.findByIdAndActiveTrue(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CLASS_NOT_FOUND, classId)));
    }

    private User findLecturerById(Long lecturerId) {
        User user = userRepository.findById(lecturerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(USER_NOT_FOUND, lecturerId)));
        if (user.getRole() != User.Role.LECTURER) {
            throw new BadRequestException("Người dùng với ID " + lecturerId + " không phải Lecturer");
        }
        return user;
    }

    private User findStudentById(Long studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(USER_NOT_FOUND, studentId)));
        if (user.getRole() != User.Role.TEAMMEMBER && user.getRole() != User.Role.TEAMLEADER) {
            throw new BadRequestException("Người dùng với ID " + studentId + " không phải Student");
        }
        return user;
    }

    private ClassResponse buildClassResponse(Class aClass, long studentCount) {
        User lecturer = aClass.getLecturer();
        return ClassResponse.builder()
                .id(aClass.getId())
                .className(aClass.getClassName())
                .classCode(aClass.getClassCode())
                .semester(aClass.getSemester())
                .lecturerId(lecturer != null ? lecturer.getId() : null)
                .lecturerName(lecturer != null ? lecturer.getFullName() : null)
                .lecturerEmail(lecturer != null ? lecturer.getEmail() : null)
                .totalStudents(studentCount)
                .active(aClass.getActive())
                .createdAt(aClass.getCreatedAt())
                .updatedAt(aClass.getUpdatedAt())
                .build();
    }

    private StudentInClassResponse buildStudentInClassResponse(ClassStudent cs) {
        return StudentInClassResponse.builder()
                .enrollmentId(cs.getId())
                .studentId(cs.getStudent().getId())
                .fullName(cs.getStudent().getFullName())
                .email(cs.getStudent().getEmail())
                .enrolledAt(cs.getEnrolledAt())
                .build();
    }
}
