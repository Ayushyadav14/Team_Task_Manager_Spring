package org.devofblue.team_task_manager_spring.service;

import org.devofblue.team_task_manager_spring.dto.request.CreateProjectRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.ProjectResponse;
import org.devofblue.team_task_manager_spring.enums.ProjectRole;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProjectService {
    ApiResponse<ProjectResponse> createProject(CreateProjectRequest request, UUID creatorId);
    ApiResponse<?> getProjects(UUID userId, Role role, Pageable pageable);
    ApiResponse<ProjectResponse> getProjectById(UUID projectId, UUID userId, Role role);
    ApiResponse<ProjectResponse> updateProject(UUID projectId, String name, String description, String status);
    ApiResponse<?> addMember(UUID projectId, String email, ProjectRole projectRole);
    ApiResponse<?> removeMember(UUID projectId, String email);
    ApiResponse<?> deleteProject(UUID projectId);
}
