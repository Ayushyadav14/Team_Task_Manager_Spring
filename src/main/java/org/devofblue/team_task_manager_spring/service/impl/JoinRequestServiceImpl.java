package org.devofblue.team_task_manager_spring.service.impl;

import lombok.RequiredArgsConstructor;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.JoinRequestResponse;
import org.devofblue.team_task_manager_spring.entity.JoinRequest;
import org.devofblue.team_task_manager_spring.entity.Project;
import org.devofblue.team_task_manager_spring.entity.ProjectMember;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.JoinRequestStatus;
import org.devofblue.team_task_manager_spring.enums.ProjectRole;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.exception.AccessDeniedException;
import org.devofblue.team_task_manager_spring.exception.BadRequestException;
import org.devofblue.team_task_manager_spring.exception.ResourceNotFoundException;
import org.devofblue.team_task_manager_spring.repository.JoinRequestRepository;
import org.devofblue.team_task_manager_spring.repository.ProjectMemberRepository;
import org.devofblue.team_task_manager_spring.repository.ProjectRepository;
import org.devofblue.team_task_manager_spring.repository.UserRepository;
import org.devofblue.team_task_manager_spring.service.JoinRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JoinRequestServiceImpl implements JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional
    public ApiResponse<JoinRequestResponse> createJoinRequest(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new BadRequestException("User is already a member of this project");
        }

        if (joinRequestRepository.existsByProjectAndUserAndStatus(project, user, JoinRequestStatus.PENDING)) {
            throw new BadRequestException("A pending join request already exists for this user");
        }

        JoinRequest request = JoinRequest.builder()
                .project(project)
                .user(user)
                .status(JoinRequestStatus.PENDING)
                .build();
        request = joinRequestRepository.save(request);

        return ApiResponse.success(mapToResponse(request), "Join request created successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<JoinRequestResponse>> getJoinRequests(UUID projectId, String status, UUID userId, Role role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        checkAdminOrOwner(project, user, role);

        List<JoinRequest> requests;
        if (status != null && !status.isEmpty()) {
            JoinRequestStatus reqStatus = JoinRequestStatus.valueOf(status.toUpperCase());
            requests = joinRequestRepository.findByProjectIdAndStatus(projectId, reqStatus);
        } else {
            requests = joinRequestRepository.findByProjectId(projectId);
        }

        List<JoinRequestResponse> responses = requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses, "Join requests retrieved successfully");
    }

    @Override
    @Transactional
    public ApiResponse<JoinRequestResponse> updateJoinRequestStatus(UUID projectId, UUID requestId, String status, UUID userId, Role role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        checkAdminOrOwner(project, user, role);

        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("JoinRequest", "id", requestId));

        if (!request.getProject().getId().equals(projectId)) {
            throw new BadRequestException("Join request does not belong to the specified project");
        }

        JoinRequestStatus newStatus = JoinRequestStatus.valueOf(status.toUpperCase());
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Can only update pending requests");
        }

        request.setStatus(newStatus);
        joinRequestRepository.save(request);

        if (newStatus == JoinRequestStatus.APPROVED) {
            ProjectMember member = ProjectMember.builder()
                    .project(project)
                    .user(request.getUser())
                    .projectRole(ProjectRole.CONTRIBUTOR)
                    .build();
            projectMemberRepository.save(member);
        }

        return ApiResponse.success(mapToResponse(request), "Join request updated successfully");
    }

    private void checkAdminOrOwner(Project project, User user, Role role) {
        if (role != Role.ADMIN) {
            ProjectMember member = projectMemberRepository.findByProjectAndUser(project, user)
                    .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));
            if (member.getProjectRole() != ProjectRole.OWNER) {
                throw new AccessDeniedException("Only project owners or admins can perform this action");
            }
        }
    }

    private JoinRequestResponse mapToResponse(JoinRequest request) {
        JoinRequestResponse.UserDto userDto = JoinRequestResponse.UserDto.builder()
                .id(request.getUser().getId())
                .name(request.getUser().getName())
                .email(request.getUser().getEmail())
                .build();

        return JoinRequestResponse.builder()
                .id(request.getId())
                .projectId(request.getProject().getId())
                .user(userDto)
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
