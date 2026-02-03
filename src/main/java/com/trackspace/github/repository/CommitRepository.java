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
public interface CommitRepository extends JpaRepository <Commit, Integer> {
    List<Commit> findByProjectId(Integer projectId);

    List<Commit> findByProjectIdAndAuthorId(Integer projectId, Integer authorId);

    List<Commit> findByProjectIdOrderByCommitDateDesc(Integer projectId);

    Optional<Commit> findByCommitSha(String commitSha);

    boolean existsByCommitSha(String commitSha);

    @Query("SELECT gc FROM Commit gc WHERE gc.projectId = :projectId " +
            "AND gc.commitDate BETWEEN :startDate AND :endDate")
    List<Commit> findByProjectIdAndDateRange(
            @Param("projectId") Integer projectId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT gc.authorId, COUNT(gc), SUM(gc.linesAdded), SUM(gc.linesDeleted) " +
            "FROM Commit gc WHERE gc.projectId = :projectId " +
            "GROUP BY gc.authorId")
    List<Object[]> getContributionStatsByProject(@Param("projectId") Integer projectId);

    @Query("SELECT COUNT(gc) FROM Commit gc WHERE gc.projectId = :projectId AND gc.authorId = :authorId")
    Long countCommitsByAuthor(@Param("projectId") Integer projectId, @Param("authorId") Integer authorId);
}
