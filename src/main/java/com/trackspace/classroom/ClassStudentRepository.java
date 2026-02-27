package com.trackspace.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ClassStudent Repository
 * Data access layer for ClassStudent entities
 */
@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    /**
     * Find all class-student records for a given student
     */

    List<ClassStudent> findByStudentId(Long studentId);

    /**
     * Check if a student is already enrolled in a class
     */
    boolean existsByClassroomIdAndStudentId(Long classId, Long studentId);

    /**
     * Find a specific class-student record
     */
    Optional<ClassStudent> findByClassroomIdAndStudentId(Long classId, Long studentId);

    /**
     * Count students in a class
     */
    long countByClassroomId(Long classId);

    /**
     * Find class-student with student info eager loaded
     */
    @Query("SELECT cs FROM ClassStudent cs JOIN FETCH cs.student WHERE cs.classroom.id = :classId")
    List<ClassStudent> findByClassIdWithStudent(@Param("classId") Long classId);
}
