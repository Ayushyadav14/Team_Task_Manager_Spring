package org.devofblue.team_task_manager_spring.controller;

import lombok.RequiredArgsConstructor;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.enums.Role;
import org.devofblue.team_task_manager_spring.security.UserPrincipal;
import org.devofblue.team_task_manager_spring.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getCurrentUser(userPrincipal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<?>> updateCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                            @RequestBody Map<String, String> request) {
        String name = request.get("name");
        String password = request.get("password");
        return ResponseEntity.ok(userService.updateCurrentUser(userPrincipal.getId(), name, password));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateUserRole(@PathVariable UUID userId,
                                                         @RequestBody Map<String, String> request) {
        Role role = Role.valueOf(request.get("role").toUpperCase());
        return ResponseEntity.ok(userService.updateUserRole(userId, role));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.deleteUser(userId));
    }
}
