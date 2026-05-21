package org.devofblue.team_task_manager_spring.service;

import org.devofblue.team_task_manager_spring.dto.request.CreateTaskRequest;
import org.devofblue.team_task_manager_spring.dto.request.UpdateTaskStatusRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.TaskResponse;
import org.devofblue.team_task_manager_spring.enums.Priority;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.enums.TaskStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {
    ApiResponse<TaskResponse> createTask(UUID projectId, CreateTaskRequest request, UUID creatorId);
    ApiResponse<?> getTasks(UUID projectId, TaskStatus status, String assigneeEmail, Priority priority, boolean overdue, Pageable pageable);
    ApiResponse<TaskResponse> getTaskById(UUID projectId, UUID taskId, UUID userId, Role role);
    ApiResponse<TaskResponse> updateTask(UUID projectId, UUID taskId, CreateTaskRequest request, UUID userId, Role role);
    ApiResponse<TaskResponse> updateTaskStatus(UUID projectId, UUID taskId, UpdateTaskStatusRequest request, UUID userId, Role role);
    ApiResponse<?> addComment(UUID projectId, UUID taskId, String text, UUID authorId);
    ApiResponse<?> deleteComment(UUID projectId, UUID taskId, UUID commentId, UUID userId, Role role);
    ApiResponse<?> deleteTask(UUID projectId, UUID taskId);
}
