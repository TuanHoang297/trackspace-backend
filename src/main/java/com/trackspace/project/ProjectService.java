package com.trackspace.project;

import com.trackspace.classroom.Group;
import com.trackspace.classroom.GroupRepository;
import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import com.trackspace.common.UnauthorizedException;
import com.trackspace.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectInfoRepository projectInfoRepository;
    private final GroupRepository groupRepository;

    private static final String PROJECT_NOT_FOUND = "Không tìm thấy project với ID: %d";
    private static final String GROUP_NOT_FOUND = "Không tìm thấy nhóm với ID: %d";

    // ==================== Project CRUD ====================

    /**
     * Create project for a group. One group can only have one project.
     * Accessible by: LECTURER (for groups in their class), TEAMLEADER (for their own group)
     */
    @Transactional
    public ProjectResponse createProject(Long groupId, CreateProjectRequest request, User currentUser) {
        Group group = findActiveGroupById(groupId);
        checkGroupAccess(group, currentUser);

        if (projectRepository.existsByGroupIdAndDeletedFalse(groupId)) {
            throw new BadRequestException("Nhóm này đã có project rồi");
        }

        Project project = Project.builder()
                .group(group)
                .projectName(request.getProjectName())
                .deleted(false)
                .build();

        return buildProjectResponse(projectRepository.save(project), false);
    }

    /**
     * Get project by group ID
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectByGroup(Long groupId) {
        findActiveGroupById(groupId);
        Project project = projectRepository.findByGroupIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm này chưa có project"));
        boolean hasInfo = projectInfoRepository.existsByProjectId(project.getId());
        return buildProjectResponse(project, hasInfo);
    }

    /**
     * Get project by project ID
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        Project project = findActiveProjectById(projectId);
        boolean hasInfo = projectInfoRepository.existsByProjectId(projectId);
        return buildProjectResponse(project, hasInfo);
    }

    /**
     * Get all projects in a class
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByClass(Long classId) {
        return projectRepository.findByClassIdAndDeletedFalse(classId).stream()
                .map(p -> buildProjectResponse(p, projectInfoRepository.existsByProjectId(p.getId())))
                .toList();
    }

    /**
     * Update project name
     */
    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, User currentUser) {
        Project project = findActiveProjectById(projectId);
        checkGroupAccess(project.getGroup(), currentUser);

        if (request.getProjectName() != null && !request.getProjectName().isBlank()) {
            project.setProjectName(request.getProjectName());
        }

        boolean hasInfo = projectInfoRepository.existsByProjectId(projectId);
        return buildProjectResponse(projectRepository.save(project), hasInfo);
    }

    /**
     * Soft-delete project
     */
    @Transactional
    public void deleteProject(Long projectId, User currentUser) {
        Project project = findActiveProjectById(projectId);
        checkGroupAccess(project.getGroup(), currentUser);
        project.setDeleted(true);
        projectRepository.save(project);
    }

    // ==================== ProjectInfo CRUD ====================

    /**
     * Create or update project info (upsert)
     */
    @Transactional
    public ProjectInfoResponse saveProjectInfo(Long projectId, ProjectInfoRequest request, User currentUser) {
        Project project = findActiveProjectById(projectId);
        checkGroupAccess(project.getGroup(), currentUser);

        ProjectInfo info = projectInfoRepository.findByProjectId(projectId)
                .orElse(ProjectInfo.builder().project(project).build());

        applyInfoUpdates(info, request);
        return buildProjectInfoResponse(projectInfoRepository.save(info));
    }

    /**
     * Get project info
     */
    @Transactional(readOnly = true)
    public ProjectInfoResponse getProjectInfo(Long projectId) {
        findActiveProjectById(projectId);
        ProjectInfo info = projectInfoRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project chưa có thông tin chi tiết"));
        return buildProjectInfoResponse(info);
    }

    /**
     * Delete project info
     */
    @Transactional
    public void deleteProjectInfo(Long projectId, User currentUser) {
        Project project = findActiveProjectById(projectId);
        checkGroupAccess(project.getGroup(), currentUser);
        ProjectInfo info = projectInfoRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project chưa có thông tin chi tiết"));
        projectInfoRepository.delete(info);
    }

    // ==================== Helpers ====================

    private Group findActiveGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .filter(g -> Boolean.TRUE.equals(g.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException(String.format(GROUP_NOT_FOUND, groupId)));
    }

    private Project findActiveProjectById(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PROJECT_NOT_FOUND, projectId)));
    }

    /**
     * LECTURER can access any group in their class.
     * TEAMLEADER can only access their own group.
     * ADMIN can access everything.
     */
    private void checkGroupAccess(Group group, User currentUser) {
        if (currentUser.getRole() == User.Role.ADMIN) return;
        if (currentUser.getRole() == User.Role.LECTURER) {
            if (!group.getClassroom().getLecturer().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("Bạn không có quyền thao tác với nhóm này");
            }
            return;
        }
        // TEAMLEADER: must be the leader of this group
        if (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Bạn không phải Team Leader của nhóm này");
        }
    }

    private void applyInfoUpdates(ProjectInfo info, ProjectInfoRequest request) {
        if (request.getTopic() != null) info.setTopic(request.getTopic());
        if (request.getContext() != null) info.setContext(request.getContext());
        if (request.getProblems() != null) info.setProblems(request.getProblems());
        if (request.getPrimaryActors() != null) info.setPrimaryActors(request.getPrimaryActors());
        if (request.getFunctionalRequirements() != null) info.setFunctionalRequirements(request.getFunctionalRequirements());
    }

    private ProjectResponse buildProjectResponse(Project project, boolean hasInfo) {
        Group group = project.getGroup();
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .classId(group.getClassroom().getId())
                .className(group.getClassroom().getClassName())
                .hasProjectInfo(hasInfo)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectInfoResponse buildProjectInfoResponse(ProjectInfo info) {
        return ProjectInfoResponse.builder()
                .id(info.getId())
                .projectId(info.getProject().getId())
                .projectName(info.getProject().getProjectName())
                .topic(info.getTopic())
                .context(info.getContext())
                .problems(info.getProblems())
                .primaryActors(info.getPrimaryActors())
                .functionalRequirements(info.getFunctionalRequirements())
                .createdAt(info.getCreatedAt())
                .updatedAt(info.getUpdatedAt())
                .build();
    }
}
