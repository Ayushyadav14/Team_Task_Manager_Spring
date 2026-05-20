package org.devofblue.team_task_manager_spring.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.devofblue.team_task_manager_spring.enums.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
