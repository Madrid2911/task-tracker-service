package com.tasktracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasktracker.domain.TaskStatus;
import com.tasktracker.dto.TaskCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class TaskControllerIntegrationTest {

    private static final String TEST_USERNAME = "test-api-user";
    private static final String TEST_PASSWORD = "test-api-password";
    private static final RequestPostProcessor AUTH = httpBasic(TEST_USERNAME, TEST_PASSWORD);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tasktracker")
            .withUsername("tasktracker")
            .withPassword("tasktracker");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("api.security.username", () -> TEST_USERNAME);
        registry.add("api.security.password", () -> TEST_PASSWORD);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTask_thenGetById_thenAssignExecutor_thenChangeStatus() throws Exception {
        TaskCreateRequest createRequest = new TaskCreateRequest("Integration task", "Created by test");

        String createResponse = mockMvc.perform(post("/api/tasks").with(AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/tasks/{id}", taskId).with(AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration task"));

        mockMvc.perform(patch("/api/tasks/{id}/assignee", taskId).with(AUTH)
                        .contentType("application/json")
                        .content("{\"assigneeId\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(1));

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId).with(AUTH)
                        .contentType("application/json")
                        .content("{\"status\": \"" + TaskStatus.IN_PROGRESS + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getTasks_returnsPaginatedResult() throws Exception {
        mockMvc.perform(get("/api/tasks").with(AUTH).param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(0)));
    }

    @Test
    void getTaskById_returns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 999_999).with(AUTH))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTasks_returns401WithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerUi_isReachableWithoutCredentials() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
