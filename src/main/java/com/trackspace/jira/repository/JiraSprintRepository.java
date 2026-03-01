package com.trackspace.jira.repository;

import com.trackspace.jira.SprintStatus;
import com.trackspace.jira.entity.JiraSprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JiraSprintRepository extends JpaRepository<JiraSprint, Integer> {
    List<JiraSprint> findByProjectIdOrderByStartDateDesc(Integer projectId);

    List<JiraSprint> findByProjectIdOrderByStartDateAsc(Integer projectId);

    List<JiraSprint> findByProjectIdAndStatus(Integer projectId, SprintStatus status);

    Optional<JiraSprint> findByJiraSprintId(String jiraSprintId);

    boolean existsByJiraSprintId(String jiraSprintId);

    long countByProjectId(Integer projectId);
}
