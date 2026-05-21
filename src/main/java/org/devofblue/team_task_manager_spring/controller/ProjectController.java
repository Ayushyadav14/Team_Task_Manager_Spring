package org.devofblue.team_task_manager_spring.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.devofblue.team_task_manager_spring.dto.request.CreateProjectRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.ProjectResponse;
import org.devofblue.team_task_manager_spring.enums.ProjectRole;
import org.devofblue.team_task_manager_spring.security.UserPrincipal;
import org.devofblue.team_task_manager_spring.service.ProjectService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(request, userPrincipal.getId()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getProjects(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.getProjects(
                userPrincipal.getId(), userPrincipal.getUser().getRole(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchProjects(@RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(projectService.searchProjects(query));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(projectService.getProjectById(
                projectId, userPrincipal.getId(), userPrincipal.getUser().getRole()));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(projectService.updateProject(
                projectId, request.get("name"), request.get("description"), request.get("status")));
    }

    @PostMapping("/{projectId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> addMember(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        ProjectRole projectRole = ProjectRole.valueOf(request.get("projectRole").toUpperCase());
        return ResponseEntity.ok(projectService.addMember(projectId, email, projectRole));
    }

    @DeleteMapping("/{projectId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> removeMember(
            @PathVariable UUID projectId,
            @RequestParam("email") String email) {
        return ResponseEntity.ok(projectService.removeMember(projectId, email));
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.deleteProject(projectId));
    }
}
