package com.tasktracker.kafka;

import com.tasktracker.kafka.event.TaskAssigneeChangedEvent;
import com.tasktracker.kafka.event.TaskCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TaskEventListener {

    private final TaskEventProducer taskEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        taskEventProducer.publishTaskCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigneeChanged(TaskAssigneeChangedEvent event) {
        taskEventProducer.publishAssigneeChanged(event);
    }
}
