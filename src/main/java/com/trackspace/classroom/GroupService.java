package com.trackspace.classroom;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Group Service
 * Business logic for group management within a class
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;

    private static final String GROUP_NOT_FOUND = "Không tìm thấy nhóm với ID: %d";
    private static final String CLASS_NOT_FOUND = "Không tìm thấy lớp học với ID: %d";
    private static final String USER_NOT_FOUND = "Không tìm thấy người dùng với ID: %d";

    // ==================== Group CRUD ====================

    @Transactional
    public GroupResponse createGroup(Long classId, CreateGroupRequest request) {
        Class aClass = findActiveClassById(classId);

        if (groupRepository.existsByClassroomIdAndGroupNameAndActiveTrue(classId, request.getGroupName())) {
            throw new BadRequestException("Tên nhóm '" + request.getGroupName() + "' đã tồn tại trong lớp này");
        }

        Group group = Group.builder()
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .classroom(aClass)
                .active(true)
                .build();

        return buildGroupResponse(groupRepository.save(group), 0L);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroupsByClass(Long classId) {
        findActiveClassById(classId);
        return groupRepository.findByClassroomIdAndActiveTrue(classId).stream()
                .map(g -> buildGroupResponse(g, groupMemberRepository.countByGroupId(g.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long classId, Long groupId) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);
        return buildGroupResponse(group, groupMemberRepository.countByGroupId(groupId));
    }

    @Transactional
    public GroupResponse updateGroup(Long classId, Long groupId, UpdateGroupRequest request) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);

        if (request.getGroupName() != null && !request.getGroupName().isBlank()) {
            if (!request.getGroupName().equals(group.getGroupName())
                    && groupRepository.existsByClassroomIdAndGroupNameAndActiveTrue(classId, request.getGroupName())) {
                throw new BadRequestException("Tên nhóm '" + request.getGroupName() + "' đã tồn tại trong lớp này");
            }
            group.setGroupName(request.getGroupName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }

        Group updated = groupRepository.save(group);
        return buildGroupResponse(updated, groupMemberRepository.countByGroupId(groupId));
    }

    @Transactional
    public void deleteGroup(Long classId, Long groupId) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);

        // Leader info is tracked via group.teamLeader, no need to change user role

        group.setActive(false);
        groupRepository.save(group);
    }

    // ==================== Leader Assignment ====================

    @Transactional
    public GroupResponse assignLeader(Long classId, Long groupId, Long studentId) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, studentId)) {
            throw new BadRequestException("Sinh viên không phải thành viên của nhóm này");
        }

        User newLeader = findUserById(studentId);
        // Just update the group's leader reference (no role change needed)
        group.setTeamLeader(newLeader);

        Group updated = groupRepository.save(group);
        return buildGroupResponse(updated, groupMemberRepository.countByGroupId(groupId));
    }

    // ==================== Member Management ====================

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long classId, Long groupId) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);

        Long leaderId = group.getTeamLeader() != null ? group.getTeamLeader().getId() : null;
        return groupMemberRepository.findByGroupIdWithMember(groupId).stream()
                .map(gm -> buildGroupMemberResponse(gm, leaderId))
                .toList();
    }

    @Transactional
    public GroupMemberResponse addMember(Long classId, Long groupId, Long studentId) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);

        // Sinh viên phải đã có trong lớp
        if (!classStudentRepository.existsByClassroomIdAndStudentId(classId, studentId)) {
            throw new BadRequestException("Sinh viên chưa được đăng ký trong lớp học này");
        }

        // Mỗi sinh viên chỉ thuộc 1 nhóm trong cùng 1 lớp
        if (groupMemberRepository.existsByClassIdAndMemberId(classId, studentId)) {
            throw new BadRequestException("Sinh viên đã thuộc một nhóm trong lớp này");
        }

        User student = findUserById(studentId);
        GroupMember saved = groupMemberRepository.save(
                GroupMember.builder().group(group).member(student).build()
        );

        Long leaderId = group.getTeamLeader() != null ? group.getTeamLeader().getId() : null;
        return buildGroupMemberResponse(saved, leaderId);
    }

    @Transactional
    public void removeMember(Long classId, Long groupId, Long studentId) {
        findActiveClassById(classId);
        Group group = findActiveGroupById(groupId);
        validateGroupBelongsToClass(group, classId);

        GroupMember membership = groupMemberRepository
                .findByGroupIdAndMemberId(groupId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên không thuộc nhóm này"));

        // If removing the team leader, clear the group's leader reference
        if (group.getTeamLeader() != null && group.getTeamLeader().getId().equals(studentId)) {
            group.setTeamLeader(null);
            groupRepository.save(group);
        }

        groupMemberRepository.delete(membership);
    }

    // ==================== Helper Methods ====================

    private Class findActiveClassById(Long classId) {
        return classRepository.findByIdAndActiveTrue(classId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CLASS_NOT_FOUND, classId)));
    }

    private Group findActiveGroupById(Long groupId) {
        return groupRepository.findByIdWithLeader(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(GROUP_NOT_FOUND, groupId)));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(USER_NOT_FOUND, userId)));
    }

    private void validateGroupBelongsToClass(Group group, Long classId) {
        if (!group.getClassroom().getId().equals(classId)) {
            throw new ResourceNotFoundException(String.format(GROUP_NOT_FOUND, group.getId()));
        }
    }



    private GroupResponse buildGroupResponse(Group group, long memberCount) {
        User leader = group.getTeamLeader();
        return GroupResponse.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .classId(group.getClassroom().getId())
                .className(group.getClassroom().getSubject() != null ? group.getClassroom().getSubject().getSubjectName() : null)
                .teamLeaderId(leader != null ? leader.getId() : null)
                .teamLeaderName(leader != null ? leader.getFullName() : null)
                .teamLeaderEmail(leader != null ? leader.getEmail() : null)
                .totalMembers(memberCount)
                .active(group.getActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private GroupMemberResponse buildGroupMemberResponse(GroupMember gm, Long leaderId) {
        User member = gm.getMember();
        return GroupMemberResponse.builder()
                .userId(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .role(member.getRole())
                .isLeader(leaderId != null && leaderId.equals(member.getId()))
                .joinedAt(gm.getJoinedAt())
                .build();
    }
}
