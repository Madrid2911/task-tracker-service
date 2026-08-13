package com.tasktracker.kafka.event;

import java.time.Instant;

public record TaskAssigneeChangedEvent(
        Long taskId,
        Long assigneeId,
        String assigneeName,
        String assigneeEmail,
        Instant occurredAt
) {
}
