package org.devofblue.team_task_manager_spring.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devofblue.team_task_manager_spring.dto.request.CreateProjectRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.ProjectResponse;
import org.devofblue.team_task_manager_spring.entity.Project;
import org.devofblue.team_task_manager_spring.entity.ProjectMember;
import org.devofblue.team_task_manager_spring.entity.Task;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.ProjectRole;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.enums.TaskStatus;
import org.devofblue.team_task_manager_spring.exception.AccessDeniedException;
import org.devofblue.team_task_manager_spring.exception.BadRequestException;
import org.devofblue.team_task_manager_spring.exception.ResourceNotFoundException;
import org.devofblue.team_task_manager_spring.repository.ProjectMemberRepository;
import org.devofblue.team_task_manager_spring.repository.ProjectRepository;
import org.devofblue.team_task_manager_spring.repository.TaskRepository;
import org.devofblue.team_task_manager_spring.repository.UserRepository;
import org.devofblue.team_task_manager_spring.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public ApiResponse<ProjectResponse> createProject(CreateProjectRequest request, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status("ACTIVE")
                .createdBy(creator)
                .build();

        project = projectRepository.save(project);

        // Add creator as OWNER
        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(creator)
                .projectRole(ProjectRole.OWNER)
                .build();
        projectMemberRepository.save(ownerMember);

        // Add additional members as CONTRIBUTOR
        if (request.getMemberEmails() != null) {
            for (String memberEmail : request.getMemberEmails()) {
                if (memberEmail == null || memberEmail.isBlank()) continue;
                if (memberEmail.equalsIgnoreCase(creator.getEmail())) continue;
                User memberUser = findUserByEmail(memberEmail);
                ProjectMember member = ProjectMember.builder()
                        .project(project)
                        .user(memberUser)
                        .projectRole(ProjectRole.CONTRIBUTOR)
                        .build();
                projectMemberRepository.save(member);
            }
        }

        return ApiResponse.success(mapToProjectResponse(project), "Project created successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> getProjects(UUID userId, Role role, Pageable pageable) {
        List<Project> projects;

        if (role == Role.ADMIN) {
            projects = projectRepository.findAll();
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            List<ProjectMember> memberships = projectMemberRepository.findAllByUser(user);
            projects = memberships.stream()
                    .map(ProjectMember::getProject)
                    .toList();
        }

        List<ProjectResponse> responses = projects.stream()
                .map(this::mapToProjectResponse)
                .toList();

        return ApiResponse.success(responses, "Projects retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ProjectResponse> getProjectById(UUID projectId, UUID userId, Role role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Check membership for non-admins
        if (role != Role.ADMIN) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            if (!projectMemberRepository.existsByProjectAndUser(project, user)) {
                throw new AccessDeniedException("You are not a member of this project");
            }
        }

        return ApiResponse.success(mapToProjectResponse(project), "Project retrieved successfully");
    }

    @Override
    @Transactional
    public ApiResponse<ProjectResponse> updateProject(UUID projectId, String name, String description, String status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (name != null && !name.isBlank()) {
            project.setName(name);
        }
        if (description != null) {
            project.setDescription(description);
        }
        if (status != null && !status.isBlank()) {
            if (!status.equals("ACTIVE") && !status.equals("ARCHIVED")) {
                throw new BadRequestException("Status must be ACTIVE or ARCHIVED");
            }
            project.setStatus(status);
        }

        project = projectRepository.save(project);
        return ApiResponse.success(mapToProjectResponse(project), "Project updated successfully");
    }

    @Override
    @Transactional
    public ApiResponse<?> addMember(UUID projectId, String email, ProjectRole projectRole) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        User user = findUserByEmail(email);

        if (projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new BadRequestException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectRole(projectRole)
                .build();
        projectMemberRepository.save(member);

        return ApiResponse.success("Member added successfully");
    }

    @Override
    @Transactional
    public ApiResponse<?> removeMember(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        User user = findUserByEmail(email);

        if (!projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new ResourceNotFoundException("ProjectMember", "email", email);
        }

        // Unassign open tasks belonging to this user in the project
        List<Task> openTasks = taskRepository.findAllByProjectAndAssigneeAndStatusNot(project, user, TaskStatus.DONE);
        for (Task task : openTasks) {
            task.setAssignee(null);
            taskRepository.save(task);
        }

        projectMemberRepository.deleteByProjectAndUser(project, user);

        return ApiResponse.success("Member removed and their open tasks unassigned");
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        projectRepository.delete(project); // cascade deletes tasks and members
        return ApiResponse.success("Project deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> searchProjects(String query) {
        List<Project> projects;
        if (query == null || query.trim().isEmpty()) {
            projects = new ArrayList<>();
        } else {
            projects = projectRepository.findByNameContainingIgnoreCase(query.trim());
        }

        List<ProjectResponse> responses = projects.stream()
                .map(this::mapToProjectResponse)
                .toList();

        return ApiResponse.success(responses, "Projects searched successfully");
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        List<ProjectMember> members = projectMemberRepository.findAllByProject(project);

        List<ProjectResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> ProjectResponse.MemberInfo.builder()
                        .userId(m.getUser().getId())
                        .name(m.getUser().getName())
                        .email(m.getUser().getEmail())
                        .projectRole(m.getProjectRole().name())
                        .build())
                .toList();

        Map<String, Long> taskCounts = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            taskCounts.put(status.name(), taskRepository.countByProjectAndStatus(project, status));
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .createdBy(project.getCreatedBy().getName())
                .members(memberInfos)
                .taskCounts(taskCounts)
                .createdAt(project.getCreatedAt())
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
