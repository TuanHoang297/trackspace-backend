package com.trackspace.classroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByActiveTrueOrderBySubjectNameAsc();
    boolean existsBySubjectCode(String subjectCode);
}
