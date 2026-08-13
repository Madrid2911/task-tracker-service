package com.tasktracker.kafka.event;

import java.time.Instant;

public record TaskCreatedEvent(
        Long taskId,
        String title,
        String description,
        Instant occurredAt
) {
}
