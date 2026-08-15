package com.tasktracker.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("api.security")
public record ApiSecurityProperties(
        @NotBlank(message = "api.security.username must not be blank") String username,
        @NotBlank(message = "api.security.password must not be blank") String password) {
}
