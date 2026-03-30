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

    @Query("SELECT g FROM Group g WHERE g.classroom.id = :classId AND g.active = true AND g.classroom.active = true")
    List<Group> findByClassroomIdAndActiveTrue(@Param("classId") Long classId);

    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.classroom.id = :classId AND g.groupName = :groupName AND g.active = true AND g.classroom.active = true")
    boolean existsByClassroomIdAndGroupNameAndActiveTrue(@Param("classId") Long classId, @Param("groupName") String groupName);

    @Query("SELECT g FROM Group g WHERE g.id = :groupId AND g.active = true AND g.classroom.active = true")
    Optional<Group> findByIdAndActiveTrue(@Param("groupId") Long groupId);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.teamLeader WHERE g.id = :id AND g.active = true AND g.classroom.active = true")
    Optional<Group> findByIdWithLeader(@Param("id") Long id);
}
