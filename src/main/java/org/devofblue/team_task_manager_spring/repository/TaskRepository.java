package org.devofblue.team_task_manager_spring.repository;

import org.devofblue.team_task_manager_spring.entity.Project;
import org.devofblue.team_task_manager_spring.entity.Task;
import org.devofblue.team_task_manager_spring.entity.User;
import org.devofblue.team_task_manager_spring.enums.Priority;
import org.devofblue.team_task_manager_spring.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findAllByProject(Project project, Pageable pageable);

    List<Task> findAllByAssignee(User assignee);

    List<Task> findAllByProjectAndAssigneeAndStatusNot(Project project, User assignee, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.project = :project" +
           " AND (:status IS NULL OR t.status = :status)" +
           " AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)" +
           " AND (:priority IS NULL OR t.priority = :priority)" +
           " AND (:overdue = false OR (t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status <> 'DONE'))")
    Page<Task> findAllWithFilters(@Param("project") Project project,
                                  @Param("status") TaskStatus status,
                                  @Param("assigneeId") UUID assigneeId,
                                  @Param("priority") Priority priority,
                                  @Param("overdue") boolean overdue,
                                  @Param("today") LocalDate today,
                                  Pageable pageable);

    List<Task> findAllByAssigneeAndStatus(User assignee, TaskStatus status);

    long countByProjectAndStatus(Project project, TaskStatus status);

    long countByProject(Project project);

    @Query("SELECT t FROM Task t WHERE t.assignee = :user AND t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status <> org.devofblue.team_task_manager_spring.enums.TaskStatus.DONE")
    List<Task> findOverdueByAssignee(@Param("user") User user, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.assignee = :user AND t.dueDate IS NOT NULL AND t.dueDate BETWEEN :start AND :end AND t.status <> org.devofblue.team_task_manager_spring.enums.TaskStatus.DONE")
    List<Task> findUpcomingByAssignee(@Param("user") User user, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status <> org.devofblue.team_task_manager_spring.enums.TaskStatus.DONE")
    List<Task> findAllOverdue(@Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.project = :project AND t.dueDate IS NOT NULL AND t.dueDate < :today AND t.status <> org.devofblue.team_task_manager_spring.enums.TaskStatus.DONE")
    List<Task> findOverdueByProject(@Param("project") Project project, @Param("today") LocalDate today);

    long countByAssignee(User assignee);

    long countByAssigneeAndStatus(User assignee, TaskStatus status);

    long countByStatus(TaskStatus status);

    List<Task> findTop5ByAssigneeOrderByUpdatedAtDesc(User assignee);
}
