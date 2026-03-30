package com.trackspace.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p JOIN FETCH p.group g JOIN FETCH g.classroom c WHERE g.id = :groupId AND p.deleted = false AND g.active = true AND c.active = true")
    Optional<Project> findByGroupIdAndDeletedFalse(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(p) > 0 FROM Project p JOIN p.group g JOIN g.classroom c WHERE g.id = :groupId AND p.deleted = false AND g.active = true AND c.active = true")
    boolean existsByGroupIdAndDeletedFalse(@Param("groupId") Long groupId);

    @Query("SELECT p FROM Project p JOIN FETCH p.group g JOIN FETCH g.classroom c WHERE c.id = :classId AND p.deleted = false AND g.active = true AND c.active = true")
    List<Project> findByClassIdAndDeletedFalse(@Param("classId") Long classId);

    @Query("SELECT p FROM Project p JOIN FETCH p.group g JOIN FETCH g.classroom c WHERE p.id = :id AND p.deleted = false AND g.active = true AND c.active = true")
    Optional<Project> findByIdAndDeletedFalse(@Param("id") Long id);
}
