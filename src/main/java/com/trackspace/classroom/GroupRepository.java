package com.trackspace.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Group Repository
 * Data access layer for Group entities
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByClassroomIdAndActiveTrue(Long classId);

    boolean existsByClassroomIdAndGroupName(Long classId, String groupName);

    Optional<Group> findByIdAndActiveTrue(Long groupId);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.teamLeader WHERE g.id = :id AND g.active = true")
    Optional<Group> findByIdWithLeader(@Param("id") Long id);
}
