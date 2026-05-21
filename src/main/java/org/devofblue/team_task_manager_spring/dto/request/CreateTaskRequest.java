package org.devofblue.team_task_manager_spring.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devofblue.team_task_manager_spring.enums.Priority;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    private String description;

    @Email(message = "Assignee email must be valid")
    private String assigneeEmail;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private LocalDate dueDate;

    private List<String> tags;
}
