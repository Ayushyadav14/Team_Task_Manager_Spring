package org.devofblue.team_task_manager_spring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private AssigneeInfo assignee;
    private LocalDate dueDate;
    private List<String> tags;
    private List<CommentInfo> comments;
    private Instant statusChangedAt;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssigneeInfo {
        private UUID id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentInfo {
        private UUID id;
        private String text;
        private UUID authorId;
        private String authorName;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
