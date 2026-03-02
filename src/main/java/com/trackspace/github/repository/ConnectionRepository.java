package com.trackspace.github.repository;

import com.trackspace.github.ConnectionStatus;
import com.trackspace.github.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Integer> {
    // Returns all connections for a project (multi-repo support)
    List<Connection> findByProjectId(Integer projectId);

    // Returns first connection (backward compat)
    Optional<Connection> findFirstByProjectIdOrderByIdAsc(Integer projectId);

    List<Connection> findByStatus(ConnectionStatus status);

    @Query("SELECT gc FROM Connection gc WHERE gc.lastSyncAt < :threshold OR gc.lastSyncAt IS NULL")
    List<Connection> findConnectionsNeedingSync(@Param("threshold") Instant threshold);

    boolean existsByProjectId(Integer projectId);

    boolean existsByProjectIdAndRepositoryUrl(Integer projectId, String repositoryUrl);
}
