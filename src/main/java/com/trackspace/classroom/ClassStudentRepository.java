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
public interface ClassStudentRepository extends JpaRepository<ClassStudent, ClassStudentId> {

    /**
     * Find all class-student records for a given student
     */

    @Query("SELECT cs FROM ClassStudent cs WHERE cs.student.id = :studentId AND cs.classroom.active = true")
    List<ClassStudent> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Check if a student is already enrolled in a class
     */
    @Query("SELECT COUNT(cs) > 0 FROM ClassStudent cs WHERE cs.classroom.id = :classId AND cs.student.id = :studentId AND cs.classroom.active = true")
    boolean existsByClassroomIdAndStudentId(@Param("classId") Long classId, @Param("studentId") Long studentId);

    /**
     * Find a specific class-student record
     */
    @Query("SELECT cs FROM ClassStudent cs WHERE cs.classroom.id = :classId AND cs.student.id = :studentId AND cs.classroom.active = true")
    Optional<ClassStudent> findByClassroomIdAndStudentId(@Param("classId") Long classId, @Param("studentId") Long studentId);

    /**
     * Count students in a class
     */
    @Query("SELECT COUNT(cs) FROM ClassStudent cs WHERE cs.classroom.id = :classId AND cs.classroom.active = true")
    long countByClassroomId(@Param("classId") Long classId);

    /**
     * Find class-student with student info eager loaded
     */
    @Query("SELECT cs FROM ClassStudent cs JOIN FETCH cs.student WHERE cs.classroom.id = :classId AND cs.classroom.active = true")
    List<ClassStudent> findByClassIdWithStudent(@Param("classId") Long classId);

    /**
     * Get all student IDs enrolled in any active class
     */
    @Query("SELECT DISTINCT cs.student.id FROM ClassStudent cs WHERE cs.classroom.active = true")
    List<Long> findAllEnrolledStudentIds();

    /**
     * Check if a student is already enrolled in another active class with the same subject and semester
     */
    @Query("SELECT COUNT(cs) > 0 FROM ClassStudent cs " +
           "WHERE cs.student.id = :studentId " +
           "AND cs.classroom.subject.id = :subjectId " +
           "AND cs.classroom.semester.id = :semesterId " +
           "AND cs.classroom.active = true")
    boolean existsByStudentAndSubjectAndSemester(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("semesterId") Long semesterId);

    /**
     * Get all student IDs enrolled in active classes with the same subject and semester
     */
    @Query("SELECT DISTINCT cs.student.id FROM ClassStudent cs " +
           "WHERE cs.classroom.subject.id = :subjectId " +
           "AND cs.classroom.semester.id = :semesterId " +
           "AND cs.classroom.active = true")
    List<Long> findStudentIdsBySubjectAndSemester(
            @Param("subjectId") Long subjectId,
            @Param("semesterId") Long semesterId);

    /**
     * Find students in a class who are already enrolled in another active class for a new subject and semester.
     * Used when updating a class's subject or semester to prevent "1 subject per semester" violations.
     */
    @Query("SELECT s FROM ClassStudent cs JOIN cs.student s " +
           "WHERE cs.classroom.id = :classId " +
           "AND s.id IN (" +
           "   SELECT cs2.student.id FROM ClassStudent cs2 " +
           "   WHERE cs2.classroom.subject.id = :newSubjectId " +
           "   AND cs2.classroom.semester.id = :newSemesterId " +
           "   AND cs2.classroom.id != :classId " +
           "   AND cs2.classroom.active = true" +
           ")")
    List<com.trackspace.user.User> findConflictingStudentsOnClassUpdate(
            @Param("classId") Long classId,
            @Param("newSubjectId") Long newSubjectId,
            @Param("newSemesterId") Long newSemesterId);
}
