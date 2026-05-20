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

        List<Task> allTasks = taskRepository.findAllByAssignee(user);

        // Tasks by status
        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            long count = allTasks.stream()
                    .filter(t -> t.getStatus() == status)
                    .count();
            tasksByStatus.put(status.name(), count);
        }

        // Overdue count
        LocalDate today = LocalDate.now();
        long overdueCount = taskRepository.findOverdueByAssignee(user, today).size();

        // Upcoming deadlines (next 7 days)
        LocalDate weekFromNow = today.plusDays(7);
        List<Task> upcomingTasks = taskRepository.findUpcomingByAssignee(user, today, weekFromNow);
        List<DashboardResponse.UpcomingTask> upcomingDeadlines = upcomingTasks.stream()
                .map(t -> DashboardResponse.UpcomingTask.builder()
                        .taskId(t.getId())
                        .title(t.getTitle())
                        .dueDate(t.getDueDate())
                        .status(t.getStatus().name())
                        .projectName(t.getProject().getName())
                        .build())
                .toList();

        DashboardResponse.MyDashboard dashboard = DashboardResponse.MyDashboard.builder()
                .tasksByStatus(tasksByStatus)
                .overdueCount(overdueCount)
                .upcomingDeadlines(upcomingDeadlines)
                .build();

        return ApiResponse.success(dashboard, "Dashboard retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DashboardResponse.AdminDashboard> getAdminDashboard() {
        long totalProjects = projectRepository.count();
        long totalUsers = userRepository.findAllByDeletedAtIsNull(null).getTotalElements();
        long totalTasks = taskRepository.count();

        LocalDate today = LocalDate.now();
        long overdueTasks = taskRepository.findAllOverdue(today).size();

        // Project summaries
        List<Project> allProjects = projectRepository.findAll();
        List<DashboardResponse.ProjectSummary> projectSummaries = allProjects.stream()
                .map(p -> DashboardResponse.ProjectSummary.builder()
                        .projectId(p.getId())
                        .projectName(p.getName())
                        .taskCount(taskRepository.countByProject(p))
                        .overdueCount(taskRepository.findOverdueByProject(p, today).size())
                        .status(p.getStatus())
                        .build())
                .toList();

        // Team workload
        List<User> activeUsers = userRepository.findAllByDeletedAtIsNull(null).getContent();
        List<DashboardResponse.TeamWorkload> teamWorkload = activeUsers.stream()
                .map(u -> {
                    List<Task> userTasks = taskRepository.findAllByAssignee(u);
                    long assigned = userTasks.size();
                    long completed = userTasks.stream()
                            .filter(t -> t.getStatus() == TaskStatus.DONE)
                            .count();
                    long overdue = taskRepository.findOverdueByAssignee(u, today).size();
                    return DashboardResponse.TeamWorkload.builder()
                            .userId(u.getId())
                            .userName(u.getName())
                            .assignedTasks(assigned)
                            .completedTasks(completed)
                            .overdueTasks(overdue)
                            .build();
                })
                .toList();

        DashboardResponse.AdminDashboard dashboard = DashboardResponse.AdminDashboard.builder()
                .totalProjects(totalProjects)
                .totalUsers(activeUsers.size())
                .totalTasks(totalTasks)
                .overdueTasks(overdueTasks)
                .projectSummaries(projectSummaries)
                .teamWorkload(teamWorkload)
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
