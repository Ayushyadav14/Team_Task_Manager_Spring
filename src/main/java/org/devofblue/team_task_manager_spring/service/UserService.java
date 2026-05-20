package org.devofblue.team_task_manager_spring.service;

import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface UserService {
    ApiResponse<?> getCurrentUser(UUID userId);
    ApiResponse<?> updateCurrentUser(UUID userId, String name, String password);
    ApiResponse<?> getAllUsers(Pageable pageable);
    ApiResponse<?> updateUserRole(UUID userId, Role role);
    ApiResponse<?> deleteUser(UUID userId);
}
