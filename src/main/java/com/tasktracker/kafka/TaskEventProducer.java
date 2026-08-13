package com.tasktracker.kafka;

import com.tasktracker.kafka.event.TaskAssigneeChangedEvent;
import com.tasktracker.kafka.event.TaskCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    public static final String TASK_CREATED_TOPIC = "task.created";
    public static final String TASK_ASSIGNEE_CHANGED_TOPIC = "task.assignee-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTaskCreated(TaskCreatedEvent event) {
        kafkaTemplate.send(TASK_CREATED_TOPIC, String.valueOf(event.taskId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TaskCreatedEvent for taskId={}", event.taskId(), ex);
                    }
                });
    }

    public void publishAssigneeChanged(TaskAssigneeChangedEvent event) {
        kafkaTemplate.send(TASK_ASSIGNEE_CHANGED_TOPIC, String.valueOf(event.taskId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TaskAssigneeChangedEvent for taskId={}", event.taskId(), ex);
                    }
                });
    }
}
