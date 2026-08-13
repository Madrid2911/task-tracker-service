package com.tasktracker.dto;

import jakarta.validation.constraints.NotNull;

public record AssigneeRequest(

        @NotNull(message = "assigneeId must not be null")
        Long assigneeId
) {
}
