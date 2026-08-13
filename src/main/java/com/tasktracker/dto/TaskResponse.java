package com.tasktracker.dto;

import com.tasktracker.domain.Task;
import com.tasktracker.domain.TaskStatus;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        UserResponse assignee
) {

    public static TaskResponse from(Task task) {
        UserResponse assignee = task.getAssignee() == null ? null : UserResponse.from(task.getAssignee());
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(), assignee);
    }
}
