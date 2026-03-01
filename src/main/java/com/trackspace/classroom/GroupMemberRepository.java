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

    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);

    long countByGroupId(Long groupId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.member WHERE gm.group.id = :groupId")
    List<GroupMember> findByGroupIdWithMember(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.group.classroom.id = :classId AND gm.member.id = :memberId")
    boolean existsByClassIdAndMemberId(@Param("classId") Long classId, @Param("memberId") Long memberId);
}
