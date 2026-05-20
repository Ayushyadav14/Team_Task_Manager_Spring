package org.devofblue.team_task_manager_spring.service;

import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.DashboardResponse;

import java.util.UUID;

public interface DashboardService {
    ApiResponse<DashboardResponse.MyDashboard> getMyDashboard(UUID userId);
    ApiResponse<DashboardResponse.AdminDashboard> getAdminDashboard();
    ApiResponse<DashboardResponse.ProjectStats> getProjectStats(UUID projectId, UUID userId, org.devofblue.team_task_manager_spring.enums.Role role);
}
