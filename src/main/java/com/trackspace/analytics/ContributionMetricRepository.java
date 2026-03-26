package com.trackspace.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionMetricRepository extends JpaRepository<ContributionMetric, Long> {

    List<ContributionMetric> findByProjectId(Integer projectId);

    Optional<ContributionMetric> findByProjectIdAndUserId(Integer projectId, Long userId);

    List<ContributionMetric> findByProjectIdOrderByContributionScoreDesc(Integer projectId);

    boolean existsByProjectIdAndUserId(Integer projectId, Long userId);

    void deleteByProjectId(Integer projectId);

    /** Members flagged as inactive within a project */
    List<ContributionMetric> findByProjectIdAndInactiveTrue(Integer projectId);

    /** Members with low contribution score */
    List<ContributionMetric> findByProjectIdAndHasLowContributionTrue(Integer projectId);

    @Query("SELECT AVG(cm.contributionScore) FROM ContributionMetric cm WHERE cm.projectId = :projectId")
    Double avgScoreByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT SUM(cm.totalCommits) FROM ContributionMetric cm WHERE cm.projectId = :projectId")
    Long sumCommitsByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT SUM(cm.linesAdded) FROM ContributionMetric cm WHERE cm.projectId = :projectId")
    Long sumLinesAddedByProjectId(@Param("projectId") Integer projectId);
}
