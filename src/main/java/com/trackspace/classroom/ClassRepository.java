package com.trackspace.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Class Repository
 * Data access layer for Class entities
 */
@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {

    /**
     * Find all active classes
     */
    List<Class> findByActiveTrue();

    /**
     * Find all classes assigned to a specific lecturer
     */
    List<Class> findByLecturerIdAndActiveTrue(Long lecturerId);

    /**
     * Check if a class name already exists
     */
    boolean existsByClassName(String className);

    /**
     * Find class by id and active status
     */
    Optional<Class> findByIdAndActiveTrue(Long id);

}
