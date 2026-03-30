package com.trackspace.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GroupMember Repository
 * Data access layer for GroupMember entities
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.member.id = :memberId AND gm.group.active = true AND gm.group.classroom.active = true")
    boolean existsByGroupIdAndMemberId(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.member.id = :memberId AND gm.group.active = true AND gm.group.classroom.active = true")
    Optional<GroupMember> findByGroupIdAndMemberId(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.group.active = true AND gm.group.classroom.active = true")
    long countByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.member WHERE gm.group.id = :groupId AND gm.group.active = true AND gm.group.classroom.active = true")
    List<GroupMember> findByGroupIdWithMember(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.group.classroom.id = :classId AND gm.member.id = :memberId AND gm.group.active = true AND gm.group.classroom.active = true")
    boolean existsByClassIdAndMemberId(@Param("classId") Long classId, @Param("memberId") Long memberId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.group WHERE gm.group.classroom.id = :classId AND gm.member.id = :memberId AND gm.group.active = true AND gm.group.classroom.active = true")
    Optional<GroupMember> findByClassIdAndMemberId(@Param("classId") Long classId, @Param("memberId") Long memberId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.group g JOIN FETCH g.classroom c LEFT JOIN FETCH c.subject WHERE gm.member.id = :memberId AND g.active = true AND c.active = true")
    List<GroupMember> findByMemberId(@Param("memberId") Long memberId);
}
