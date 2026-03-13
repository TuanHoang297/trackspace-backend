package com.trackspace.srs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SrsDocumentRepository extends JpaRepository<SrsDocument, Long> {
    Optional<SrsDocument> findFirstByProjectIdOrderByVersionNumberDesc(Long projectId);
    List<SrsDocument> findByProjectIdOrderByVersionNumberDesc(Long projectId);

    @Query("SELECT COALESCE(MAX(s.versionNumber), 0) FROM SrsDocument s WHERE s.project.id = :projectId")
    Integer findMaxVersionNumberByProjectId(@Param("projectId") Long projectId);
}
