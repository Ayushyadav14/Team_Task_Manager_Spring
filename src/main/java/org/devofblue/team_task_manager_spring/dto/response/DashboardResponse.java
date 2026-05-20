package org.devofblue.team_task_manager_spring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DashboardResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyDashboard {
        private Map<String, Long> tasksByStatus;
        private long overdueCount;
        private List<UpcomingTask> upcomingDeadlines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboard {
        private long totalProjects;
        private long totalUsers;
        private long totalTasks;
        private long overdueTasks;
        private List<ProjectSummary> projectSummaries;
        private List<TeamWorkload> teamWorkload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStats {
        private UUID projectId;
        private String projectName;
        private Map<String, Long> statusBreakdown;
        private long totalTasks;
        private double completionPercentage;
        private double overduePercentage;
        private List<ActivityEntry> activityTimeline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingTask {
        private UUID taskId;
        private String title;
        private LocalDate dueDate;
        private String status;
        private String projectName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private UUID projectId;
        private String projectName;
        private long taskCount;
        private long overdueCount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamWorkload {
        private UUID userId;
        private String userName;
        private long assignedTasks;
        private long completedTasks;
        private long overdueTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityEntry {
        private UUID taskId;
        private String taskTitle;
        private String action;
        private java.time.Instant timestamp;
    }
}
