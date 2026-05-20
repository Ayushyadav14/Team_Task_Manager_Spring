package org.devofblue.team_task_manager_spring.enums;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE;

    /**
     * Enforces strict linear transitions: TODO → IN_PROGRESS → IN_REVIEW → DONE.
     * No skipping steps, no going back.
     */
    public boolean canTransitionTo(TaskStatus next) {
        return next.ordinal() == this.ordinal() + 1;
    }
}
