package org.devofblue.team_task_manager_spring.repository;

import org.devofblue.team_task_manager_spring.entity.Comment;
import org.devofblue.team_task_manager_spring.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findAllByTaskOrderByCreatedAtAsc(Task task);
}
