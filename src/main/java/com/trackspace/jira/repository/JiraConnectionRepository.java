package com.trackspace.jira.repository;

import com.trackspace.jira.JiraConnectionStatus;
import com.trackspace.jira.entity.JiraConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JiraConnectionRepository extends JpaRepository<JiraConnection, Integer> {
    Optional<JiraConnection> findByProjectId(Integer projectId);

    Optional<JiraConnection> findByProjectKey(String projectKey);

    boolean existsByProjectId(Integer projectId);

    List<JiraConnection> findByConnectionStatus(JiraConnectionStatus status);

    @Query("SELECT jc FROM JiraConnection jc WHERE jc.lastSyncAt < :threshold OR jc.lastSyncAt IS NULL")
    List<JiraConnection> findConnectionsNeedingSync(@Param("threshold") Instant threshold);
}
