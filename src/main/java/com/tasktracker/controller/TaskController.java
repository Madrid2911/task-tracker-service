package com.tasktracker.controller;

import com.tasktracker.dto.AssigneeRequest;
import com.tasktracker.dto.StatusChangeRequest;
import com.tasktracker.dto.TaskCreateRequest;
import com.tasktracker.dto.TaskResponse;
import com.tasktracker.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management API")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get tasks with pagination")
    public Page<TaskResponse> getTasks(@PageableDefault(size = 20) Pageable pageable) {
        return taskService.getTasks(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by id")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse created = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/assignee")
    @Operation(summary = "Assign an executor to a task")
    public TaskResponse assignExecutor(@PathVariable Long id, @Valid @RequestBody AssigneeRequest request) {
        return taskService.assignExecutor(id, request.assigneeId());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change task status")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return taskService.changeStatus(id, request.status());
    }
}
