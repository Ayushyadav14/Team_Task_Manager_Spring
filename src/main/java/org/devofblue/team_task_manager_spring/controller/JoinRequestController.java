package org.devofblue.team_task_manager_spring.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.devofblue.team_task_manager_spring.dto.request.JoinRequestStatusUpdateRequest;
import org.devofblue.team_task_manager_spring.dto.response.ApiResponse;
import org.devofblue.team_task_manager_spring.dto.response.JoinRequestResponse;
import org.devofblue.team_task_manager_spring.security.UserPrincipal;
import org.devofblue.team_task_manager_spring.service.JoinRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<JoinRequestResponse>> requestJoin(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(joinRequestService.createJoinRequest(projectId, userPrincipal.getId()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JoinRequestResponse>>> getJoinRequests(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(joinRequestService.getJoinRequests(
                projectId, status, userPrincipal.getId(), userPrincipal.getUser().getRole()));
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> updateJoinRequestStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID requestId,
            @Valid @RequestBody JoinRequestStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(joinRequestService.updateJoinRequestStatus(
                projectId, requestId, request.getStatus(), userPrincipal.getId(), userPrincipal.getUser().getRole()));
    }
}
