package org.devofblue.team_task_manager_spring.service.impl;

import lombok.RequiredArgsConstructor;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.exception.BadRequestException;
import org.devofblue.team_task_manager_spring.exception.ResourceNotFoundException;
import org.devofblue.team_task_manager_spring.repository.UserRepository;
import org.devofblue.team_task_manager_spring.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> getCurrentUser(UUID userId) {
        User user = findActiveUser(userId);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());
        profile.put("createdAt", user.getCreatedAt());

        return ApiResponse.success(profile, "User profile retrieved");
    }

    @Override
    @Transactional
    public ApiResponse<?> updateCurrentUser(UUID userId, String name, String password) {
        User user = findActiveUser(userId);

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        userRepository.save(user);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());

        return ApiResponse.success(profile, "User updated successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAllByDeletedAtIsNull(pageable);

        var content = users.getContent().stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole().name());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).toList();

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", content);
        page.put("page", users.getNumber());
        page.put("size", users.getSize());
        page.put("totalPages", users.getTotalPages());
        page.put("totalElements", users.getTotalElements());

        return ApiResponse.success(page, "Users retrieved successfully");
    }

    @Override
    @Transactional
    public ApiResponse<?> updateUserRole(UUID userId, Role role) {
        User user = findActiveUser(userId);
        user.setRole(role);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("role", user.getRole().name());

        return ApiResponse.success(result, "User role updated to " + role.name());
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteUser(UUID userId) {
        User user = findActiveUser(userId);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        return ApiResponse.success("User soft-deleted successfully");
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
