package org.devofblue.team_task_manager_spring.service;

import org.devofblue.team_task_manager_spring.dto.request.LoginRequest;
import org.devofblue.team_task_manager_spring.dto.request.RegisterRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.AuthResponse;

public interface AuthService {
    ApiResponse<AuthResponse> register(RegisterRequest request);
    ApiResponse<AuthResponse> login(LoginRequest request);
    ApiResponse<AuthResponse> refreshToken(String refreshToken);
    ApiResponse<Void> logout(String refreshToken);
}
