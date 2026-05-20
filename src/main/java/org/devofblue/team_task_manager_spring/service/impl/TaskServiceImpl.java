package org.devofblue.team_task_manager_spring.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devofblue.team_task_manager_spring.dto.request.CreateTaskRequest;
import org.devofblue.team_task_manager_spring.dto.request.UpdateTaskStatusRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.TaskResponse;
import org.devofblue.team_task_manager_spring.entity.*;
import org.devofblue.team_task_manager_spring.enums.Priority;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.enums.TaskStatus;
import org.devofblue.team_task_manager_spring.exception.AccessDeniedException;
import org.devofblue.team_task_manager_spring.exception.BadRequestException;
import org.devofblue.team_task_manager_spring.exception.ResourceNotFoundException;
import org.devofblue.team_task_manager_spring.repository.*;
import org.devofblue.team_task_manager_spring.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ApiResponse<TaskResponse> createTask(UUID projectId, CreateTaskRequest request, UUID creatorId) {
        Project project = findProject(projectId);
        User creator = findUser(creatorId);
        checkMembership(project, creator);

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = findUser(request.getAssigneeId());
            checkMembership(project, assignee);
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .assignee(assignee)
                .project(project)
                .dueDate(request.getDueDate())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .createdBy(creator)
                .build();

        task = taskRepository.save(task);
        return ApiResponse.success(mapToTaskResponse(task, false), "Task created successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> getTasks(UUID projectId, TaskStatus status, UUID assigneeId,
                                   Priority priority, boolean overdue, Pageable pageable) {
        Project project = findProject(projectId);

        Page<Task> tasks = taskRepository.findAllWithFilters(
                project, status, assigneeId, priority, overdue, LocalDate.now(), pageable);

        var content = tasks.getContent().stream()
                .map(t -> mapToTaskResponse(t, false))
                .toList();

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", content);
        page.put("page", tasks.getNumber());
        page.put("size", tasks.getSize());
        page.put("totalPages", tasks.getTotalPages());
        page.put("totalElements", tasks.getTotalElements());

        return ApiResponse.success(page, "Tasks retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<TaskResponse> getTaskById(UUID projectId, UUID taskId, UUID userId, Role role) {
        Project project = findProject(projectId);
        if (role != Role.ADMIN) {
            checkMembership(project, findUser(userId));
        }

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        return ApiResponse.success(mapToTaskResponse(task, true), "Task retrieved successfully");
    }

    @Override
    @Transactional
    public ApiResponse<TaskResponse> updateTask(UUID projectId, UUID taskId, CreateTaskRequest request,
                                                UUID userId, Role role) {
        Project project = findProject(projectId);
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Members can only update their own assigned tasks
        if (role != Role.ADMIN) {
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
                throw new AccessDeniedException("You can only edit tasks assigned to you");
            }
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getTags() != null) {
            task.setTags(request.getTags());
        }
        if (request.getAssigneeId() != null) {
            User newAssignee = findUser(request.getAssigneeId());
            checkMembership(project, newAssignee);
            task.setAssignee(newAssignee);
        }

        task = taskRepository.save(task);
        return ApiResponse.success(mapToTaskResponse(task, false), "Task updated successfully");
    }

    @Override
    @Transactional
    public ApiResponse<TaskResponse> updateTaskStatus(UUID projectId, UUID taskId,
                                                      UpdateTaskStatusRequest request, UUID userId, Role role) {
        Project project = findProject(projectId);
        if (role != Role.ADMIN) {
            checkMembership(project, findUser(userId));
        }

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Members can only update status of their own tasks
        if (role != Role.ADMIN) {
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
                throw new AccessDeniedException("You can only update status of tasks assigned to you");
            }
        }

        TaskStatus currentStatus = task.getStatus();
        TaskStatus newStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s. Must follow: TODO → IN_PROGRESS → IN_REVIEW → DONE",
                            currentStatus, newStatus));
        }

        task.setStatus(newStatus);
        task.setStatusChangedAt(Instant.now());
        task = taskRepository.save(task);

        return ApiResponse.success(mapToTaskResponse(task, false), "Task status updated to " + newStatus);
    }

    @Override
    @Transactional
    public ApiResponse<?> addComment(UUID projectId, UUID taskId, String text, UUID authorId) {
        Project project = findProject(projectId);
        User author = findUser(authorId);
        checkMembership(project, author);

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        Comment comment = Comment.builder()
                .text(text)
                .task(task)
                .author(author)
                .build();

        commentRepository.save(comment);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("text", comment.getText());
        result.put("authorId", author.getId());
        result.put("authorName", author.getName());
        result.put("createdAt", comment.getCreatedAt());

        return ApiResponse.success(result, "Comment added successfully");
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteComment(UUID projectId, UUID taskId, UUID commentId, UUID userId, Role role) {
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        Comment comment = commentRepository.findById(commentId)
                .filter(c -> c.getTask().getId().equals(taskId))
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        // Members can only delete their own comments; admins can delete any
        if (role != Role.ADMIN && !comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
        return ApiResponse.success("Comment deleted successfully");
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteTask(UUID projectId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        taskRepository.delete(task);
        return ApiResponse.success("Task deleted successfully");
    }

    // --- Helpers ---

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void checkMembership(Project project, User user) {
        if (!projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new AccessDeniedException("User is not a member of this project");
        }
    }

    private TaskResponse mapToTaskResponse(Task task, boolean includeComments) {
        TaskResponse.AssigneeInfo assigneeInfo = null;
        if (task.getAssignee() != null) {
            assigneeInfo = TaskResponse.AssigneeInfo.builder()
                    .id(task.getAssignee().getId())
                    .name(task.getAssignee().getName())
                    .email(task.getAssignee().getEmail())
                    .build();
        }

        List<TaskResponse.CommentInfo> commentInfos = null;
        if (includeComments) {
            List<Comment> comments = commentRepository.findAllByTaskOrderByCreatedAtAsc(task);
            commentInfos = comments.stream()
                    .map(c -> TaskResponse.CommentInfo.builder()
                            .id(c.getId())
                            .text(c.getText())
                            .authorId(c.getAuthor().getId())
                            .authorName(c.getAuthor().getName())
                            .createdAt(c.getCreatedAt())
                            .updatedAt(c.getUpdatedAt())
                            .build())
                    .toList();
        }

        List<String> tags = task.getTags() == null ? java.util.List.of() : new java.util.ArrayList<>(task.getTags());

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .assignee(assigneeInfo)
                .dueDate(task.getDueDate())
                .tags(tags)
                .comments(commentInfos)
                .statusChangedAt(task.getStatusChangedAt())
                .createdBy(task.getCreatedBy().getName())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
