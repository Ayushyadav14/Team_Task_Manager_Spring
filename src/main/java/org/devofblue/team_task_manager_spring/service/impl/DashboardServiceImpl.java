package org.devofblue.team_task_manager_spring.service.impl;

import lombok.RequiredArgsConstructor;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.DashboardResponse;
import org.devofblue.team_task_manager_spring.entity.Project;
import org.devofblue.team_task_manager_spring.entity.Task;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.enums.TaskStatus;
import org.devofblue.team_task_manager_spring.exception.AccessDeniedException;
import org.devofblue.team_task_manager_spring.exception.ResourceNotFoundException;
import org.devofblue.team_task_manager_spring.repository.*;
import org.devofblue.team_task_manager_spring.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DashboardResponse.MyDashboard> getMyDashboard(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long totalProjects = projectMemberRepository.findAllByUser(user).size();
        long totalTasks = taskRepository.countByAssignee(user);
        long completedTasks = taskRepository.countByAssigneeAndStatus(user, TaskStatus.DONE);

        LocalDate today = LocalDate.now();
        List<Task> overdueTaskList = taskRepository.findOverdueByAssignee(user, today);
        long overdueTasksCount = overdueTaskList.size();

        Map<String, Long> taskSummary = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            taskSummary.put(status.name(), taskRepository.countByAssigneeAndStatus(user, status));
        }

        List<Task> recent = taskRepository.findTop5ByAssigneeOrderByUpdatedAtDesc(user);
        List<DashboardResponse.DashboardTask> recentTasks = recent.stream()
                .map(t -> DashboardResponse.DashboardTask.builder()
                        .id(t.getId().toString())
                        .title(t.getTitle())
                        .status(t.getStatus().name())
                        .dueDate(t.getDueDate())
                        .build())
                .toList();

        List<DashboardResponse.DashboardTask> overdueTasksList = overdueTaskList.stream()
                .map(t -> DashboardResponse.DashboardTask.builder()
                        .id(t.getId().toString())
                        .title(t.getTitle())
                        .status(t.getStatus().name())
                        .dueDate(t.getDueDate())
                        .build())
                .toList();

        DashboardResponse.MyDashboard dashboard = DashboardResponse.MyDashboard.builder()
                .totalProjects(totalProjects)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasksCount)
                .taskSummary(taskSummary)
                .recentTasks(recentTasks)
                .overdueTasksList(overdueTasksList)
                .build();

        return ApiResponse.success(dashboard, "Dashboard retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DashboardResponse.AdminDashboard> getAdminDashboard() {
        long totalProjects = projectRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus(TaskStatus.DONE);

        LocalDate today = LocalDate.now();
        long overdueTasksCount = taskRepository.findAllOverdue(today).size();

        Map<String, Long> taskSummary = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            taskSummary.put(status.name(), taskRepository.countByStatus(status));
        }

        List<Project> recent = projectRepository.findTop5ByOrderByCreatedAtDesc();
        List<DashboardResponse.DashboardProject> recentProjects = recent.stream()
                .map(p -> DashboardResponse.DashboardProject.builder()
                        .id(p.getId().toString())
                        .name(p.getName())
                        .status(p.getStatus())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();

        DashboardResponse.AdminDashboard dashboard = DashboardResponse.AdminDashboard.builder()
                .totalProjects(totalProjects)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasksCount)
                .taskSummary(taskSummary)
                .recentProjects(recentProjects)
                .build();

        return ApiResponse.success(dashboard, "Admin dashboard retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DashboardResponse.ProjectStats> getProjectStats(UUID projectId, UUID userId, Role role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Membership check for non-admins
        if (role != Role.ADMIN) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            if (!projectMemberRepository.existsByProjectAndUser(project, user)) {
                throw new AccessDeniedException("You are not a member of this project");
            }
        }

        // Status breakdown
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        long totalTasks = 0;
        for (TaskStatus status : TaskStatus.values()) {
            long count = taskRepository.countByProjectAndStatus(project, status);
            statusBreakdown.put(status.name(), count);
            totalTasks += count;
        }

        // Completion & overdue percentages
        long doneTasks = statusBreakdown.getOrDefault("DONE", 0L);
        double completionPercentage = totalTasks > 0 ? (doneTasks * 100.0 / totalTasks) : 0.0;

        LocalDate today = LocalDate.now();
        long overdueTasks = taskRepository.findOverdueByProject(project, today).size();
        double overduePercentage = totalTasks > 0 ? (overdueTasks * 100.0 / totalTasks) : 0.0;

        // Activity timeline — recent status changes
        List<Task> projectTasks = taskRepository.findAllByProject(project, null).getContent();
        List<DashboardResponse.ActivityEntry> activityTimeline = projectTasks.stream()
                .filter(t -> t.getStatusChangedAt() != null)
                .sorted(Comparator.comparing(Task::getStatusChangedAt).reversed())
                .limit(20)
                .map(t -> DashboardResponse.ActivityEntry.builder()
                        .taskId(t.getId())
                        .taskTitle(t.getTitle())
                        .action("Status changed to " + t.getStatus().name())
                        .timestamp(t.getStatusChangedAt())
                        .build())
                .toList();

        DashboardResponse.ProjectStats stats = DashboardResponse.ProjectStats.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .statusBreakdown(statusBreakdown)
                .totalTasks(totalTasks)
                .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                .overduePercentage(Math.round(overduePercentage * 100.0) / 100.0)
                .activityTimeline(activityTimeline)
                .build();

        return ApiResponse.success(stats, "Project stats retrieved successfully");
    }
}
