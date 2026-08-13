package com.tasktracker.dto;

import com.tasktracker.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(

        @NotNull(message = "status must not be null")
        TaskStatus status
) {
}
