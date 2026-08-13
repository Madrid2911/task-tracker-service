package com.tasktracker.service;

import com.tasktracker.domain.Task;
import com.tasktracker.domain.TaskStatus;
import com.tasktracker.domain.User;
import com.tasktracker.dto.TaskCreateRequest;
import com.tasktracker.dto.TaskResponse;
import com.tasktracker.exception.TaskNotFoundException;
import com.tasktracker.exception.UserNotFoundException;
import com.tasktracker.kafka.event.TaskAssigneeChangedEvent;
import com.tasktracker.kafka.event.TaskCreatedEvent;
import com.tasktracker.repository.TaskRepository;
import com.tasktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Page<TaskResponse> getTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(TaskResponse::from);
    }

    public TaskResponse getTaskById(Long id) {
        return taskRepository.findById(id)
                .map(TaskResponse::from)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.NEW);
        task = taskRepository.save(task);

        eventPublisher.publishEvent(
                new TaskCreatedEvent(task.getId(), task.getTitle(), task.getDescription(), Instant.now()));

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse assignExecutor(Long taskId, Long assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new UserNotFoundException(assigneeId));

        task.setAssignee(assignee);
        task = taskRepository.save(task);

        eventPublisher.publishEvent(
                new TaskAssigneeChangedEvent(task.getId(), assignee.getId(), assignee.getName(),
                        assignee.getEmail(), Instant.now()));

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setStatus(status);
        task = taskRepository.save(task);

        return TaskResponse.from(task);
    }
}
