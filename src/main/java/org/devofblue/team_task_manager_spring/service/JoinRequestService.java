package org.devofblue.team_task_manager_spring.service;

import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.JoinRequestResponse;
import org.devofblue.team_task_manager_spring.enums.Role;

import java.util.List;
import java.util.UUID;

public interface JoinRequestService {
    ApiResponse<JoinRequestResponse> createJoinRequest(UUID projectId, UUID userId);
    ApiResponse<List<JoinRequestResponse>> getJoinRequests(UUID projectId, String status, UUID userId, Role role);
    ApiResponse<JoinRequestResponse> updateJoinRequestStatus(UUID projectId, UUID requestId, String status, UUID userId, Role role);
}
