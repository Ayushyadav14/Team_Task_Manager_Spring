package org.devofblue.team_task_manager_spring.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestStatusUpdateRequest {
    @NotBlank
    private String status;
}
