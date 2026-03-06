package com.trackspace.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByActiveTrueOrderByCreatedAtDesc();
    boolean existsByName(String name);
}
