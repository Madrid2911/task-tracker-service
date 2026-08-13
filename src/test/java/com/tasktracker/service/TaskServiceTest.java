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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository, eventPublisher);
    }

    @Test
    void createTask_savesTaskWithNewStatusAndPublishesEvent() {
        TaskCreateRequest request = new TaskCreateRequest("Write report", "Quarterly report");
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        TaskResponse response = taskService.createTask(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Write report");
        assertThat(response.status()).isEqualTo(TaskStatus.NEW);

        ArgumentCaptor<TaskCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().taskId()).isEqualTo(1L);
    }

    @Test
    void getTaskById_throwsWhenMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void assignExecutor_setsAssigneeAndPublishesEvent() {
        Task task = new Task();
        task.setId(5L);
        task.setTitle("Fix bug");
        task.setStatus(TaskStatus.NEW);

        User user = new User();
        user.setId(2L);
        user.setName("Alice Ivanova");
        user.setEmail("alice@example.com");

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.assignExecutor(5L, 2L);

        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().id()).isEqualTo(2L);

        ArgumentCaptor<TaskAssigneeChangedEvent> eventCaptor = ArgumentCaptor.forClass(TaskAssigneeChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().assigneeId()).isEqualTo(2L);
    }

    @Test
    void assignExecutor_throwsWhenUserMissing() {
        Task task = new Task();
        task.setId(5L);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignExecutor(5L, 2L))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void changeStatus_updatesStatus() {
        Task task = new Task();
        task.setId(7L);
        task.setStatus(TaskStatus.NEW);

        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.changeStatus(7L, TaskStatus.DONE);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }
}
