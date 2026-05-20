package org.devofblue.team_task_manager_spring.repository;

import org.devofblue.team_task_manager_spring.entity.Project;
import org.devofblue.team_task_manager_spring.entity.ProjectMember;
import org.devofblue.team_task_manager_spring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    boolean existsByProjectAndUser(Project project, User user);
    List<ProjectMember> findAllByProject(Project project);
    List<ProjectMember> findAllByUser(User user);
    void deleteByProjectAndUser(Project project, User user);
}
