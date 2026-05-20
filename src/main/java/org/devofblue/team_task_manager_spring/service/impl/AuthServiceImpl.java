package org.devofblue.team_task_manager_spring.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devofblue.team_task_manager_spring.dto.request.LoginRequest;
import org.devofblue.team_task_manager_spring.dto.request.RegisterRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.AuthResponse;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.exception.BadRequestException;
import org.devofblue.team_task_manager_spring.repository.UserRepository;
import org.devofblue.team_task_manager_spring.security.JwtTokenProvider;
import org.devofblue.team_task_manager_spring.security.UserPrincipal;
import org.devofblue.team_task_manager_spring.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // In-memory blacklist for refresh tokens (lost on restart)
    private final Set<String> blacklistedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    @Transactional
    public ApiResponse<AuthResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.MEMBER)
                .build();

        user = userRepository.save(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ApiResponse.success(response, "User registered successfully");
    }

    @Override
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

        AuthResponse response = AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .role(userPrincipal.getUser().getRole().name())
                .build();

        return ApiResponse.success(response, "Login successful");
    }

    @Override
    public ApiResponse<AuthResponse> refreshToken(String refreshToken) {
        if (blacklistedTokens.contains(refreshToken)) {
            throw new BadRequestException("Refresh token has been revoked");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        var userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);

        AuthResponse response = AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ApiResponse.success(response, "Token refreshed successfully");
    }

    @Override
    public ApiResponse<Void> logout(String refreshToken) {
        blacklistedTokens.add(refreshToken);
        return ApiResponse.success("Logged out successfully");
    }
}
