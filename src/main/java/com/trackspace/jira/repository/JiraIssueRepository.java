package com.trackspace.jira.repository;

import com.trackspace.jira.entity.JiraIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JiraIssueRepository extends JpaRepository<JiraIssue, Integer> {
    List<JiraIssue> findByProjectId(Integer projectId);

    List<JiraIssue> findByProjectIdAndSprintId(Integer projectId, Integer sprintId);

    List<JiraIssue> findByProjectIdAndStatus(Integer projectId, String status);

    List<JiraIssue> findByProjectIdAndAssigneeId(Integer projectId, Integer assigneeId);

    Optional<JiraIssue> findByJiraIssueId(String jiraIssueId);

    Optional<JiraIssue> findByIssueKey(String issueKey);

    boolean existsByJiraIssueId(String jiraIssueId);

    long countByProjectId(Integer projectId);

    @Query("SELECT ji FROM JiraIssue ji WHERE ji.projectId = :projectId " +
            "AND (:sprintId IS NULL OR ji.sprintId = :sprintId) " +
            "AND (:status IS NULL OR ji.status = :status) " +
            "AND (:assigneeId IS NULL OR ji.assigneeId = :assigneeId)")
    List<JiraIssue> findByFilters(
            @Param("projectId") Integer projectId,
            @Param("sprintId") Integer sprintId,
            @Param("status") String status,
            @Param("assigneeId") Integer assigneeId);

    @Query("SELECT ji.status, COUNT(ji) FROM JiraIssue ji " +
            "WHERE ji.projectId = :projectId GROUP BY ji.status")
    List<Object[]> countByProjectIdGroupByStatus(@Param("projectId") Integer projectId);

    @Query("SELECT ji.assigneeId, COUNT(ji) FROM JiraIssue ji " +
            "WHERE ji.projectId = :projectId AND ji.status = 'Done' GROUP BY ji.assigneeId")
    List<Object[]> countCompletedByAssignee(@Param("projectId") Integer projectId);
}
