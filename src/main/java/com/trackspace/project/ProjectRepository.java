package com.trackspace.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByGroupIdAndDeletedFalse(Long groupId);

    boolean existsByGroupIdAndDeletedFalse(Long groupId);

    @Query("SELECT p FROM Project p JOIN FETCH p.group g JOIN FETCH g.classroom c WHERE c.id = :classId AND p.deleted = false")
    List<Project> findByClassIdAndDeletedFalse(@Param("classId") Long classId);

    Optional<Project> findByIdAndDeletedFalse(Long id);
}
