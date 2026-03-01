package com.trackspace.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectInfoRepository extends JpaRepository<ProjectInfo, Long> {

    Optional<ProjectInfo> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}
