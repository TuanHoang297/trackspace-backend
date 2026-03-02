package com.trackspace.github.repository;

import com.trackspace.github.entity.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommitRepository extends JpaRepository<Commit, Integer> {
        List<Commit> findByProjectId(Integer projectId);

        List<Commit> findByProjectIdAndAuthorId(Integer projectId, Integer authorId);

        List<Commit> findByProjectIdOrderByCommitDateDesc(Integer projectId);

        Optional<Commit> findByCommitSha(String commitSha);

        boolean existsByCommitSha(String commitSha);

        @Query("SELECT gc.commitSha FROM Commit gc WHERE gc.projectId = :projectId")
        List<String> findAllShasByProjectId(@Param("projectId") Integer projectId);

        @Query("SELECT gc FROM Commit gc WHERE gc.projectId = :projectId " +
                        "AND gc.commitDate BETWEEN :startDate AND :endDate")
        List<Commit> findByProjectIdAndDateRange(
                        @Param("projectId") Integer projectId,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        @Query("SELECT COUNT(gc) FROM Commit gc WHERE gc.projectId = :projectId AND gc.authorId = :authorId")
        Long countCommitsByAuthor(@Param("projectId") Integer projectId, @Param("authorId") Integer authorId);

        @Query("SELECT gc FROM Commit gc WHERE gc.projectId = :projectId " +
                        "AND gc.branchName = :branch " +
                        "ORDER BY gc.commitDate DESC")
        List<Commit> findByProjectIdAndBranchContaining(
                        @Param("projectId") Integer projectId,
                        @Param("branch") String branch);

        @Query("SELECT gc FROM Commit gc WHERE gc.projectId = :projectId " +
                        "AND gc.branchName = :branch " +
                        "AND gc.commitDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY gc.commitDate DESC")
        List<Commit> findByProjectIdAndBranchContainingAndDateRange(
                        @Param("projectId") Integer projectId,
                        @Param("branch") String branch,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        // ── connectionId-scoped queries ──

        Long countByConnectionId(Integer connectionId);

        List<Commit> findByConnectionIdOrderByCommitDateDesc(Integer connectionId);

        @Query("SELECT gc.commitSha FROM Commit gc WHERE gc.connectionId = :connectionId")
        List<String> findAllShasByConnectionId(@Param("connectionId") Integer connectionId);

        @Query("SELECT gc FROM Commit gc WHERE gc.connectionId = :connectionId " +
                        "AND gc.branchName = :branch ORDER BY gc.commitDate DESC")
        List<Commit> findByConnectionIdAndBranch(
                        @Param("connectionId") Integer connectionId,
                        @Param("branch") String branch);

        @Query("SELECT gc FROM Commit gc WHERE gc.connectionId = :connectionId " +
                        "AND gc.branchName = :branch " +
                        "AND gc.commitDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY gc.commitDate DESC")
        List<Commit> findByConnectionIdAndBranchAndDateRange(
                        @Param("connectionId") Integer connectionId,
                        @Param("branch") String branch,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        @Query("SELECT gc FROM Commit gc WHERE gc.connectionId = :connectionId " +
                        "AND gc.commitDate BETWEEN :startDate AND :endDate")
        List<Commit> findByConnectionIdAndDateRange(
                        @Param("connectionId") Integer connectionId,
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);

        List<Commit> findByConnectionIdAndAuthorId(Integer connectionId, Integer authorId);
}
