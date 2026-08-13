package com.tasktracker.controller;

import com.tasktracker.dto.UserResponse;
import com.tasktracker.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Read-only user lookup (onboarding is out of scope)")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List users available as task assignees")
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }
}
