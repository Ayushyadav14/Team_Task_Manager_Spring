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
        private long totalProjects;
        private long totalTasks;
        private long completedTasks;
        private long overdueTasks;
        private Map<String, Long> taskSummary;
        private List<DashboardTask> recentTasks;
        private List<DashboardTask> overdueTasksList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboard {
        private long totalProjects;
        private long totalTasks;
        private long completedTasks;
        private long overdueTasks;
        private Map<String, Long> taskSummary;
        private List<DashboardProject> recentProjects;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardTask {
        private String id; // The requirements show id as string (UUID)
        private String title;
        private String status;
        private LocalDate dueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardProject {
        private String id;
        private String name;
        private String status;
        private java.time.Instant createdAt;
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
