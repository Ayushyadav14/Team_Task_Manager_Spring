package org.devofblue.team_task_manager_spring.repository;

import org.devofblue.team_task_manager_spring.entity.JoinRequest;
import org.devofblue.team_task_manager_spring.entity.Project;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {
    List<JoinRequest> findByProjectId(UUID projectId);
    List<JoinRequest> findByProjectIdAndStatus(UUID projectId, JoinRequestStatus status);
    Optional<JoinRequest> findByProjectAndUser(Project project, User user);
    boolean existsByProjectAndUserAndStatus(Project project, User user, JoinRequestStatus status);
}
