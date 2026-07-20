package com.showcase.backend.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.backend.config.SecurityConfig;
import com.showcase.backend.exception.TaskNotFoundException;
import com.showcase.backend.task.dto.TaskRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, TaskControllerWebMvcTest.NoOpJwtDecoderConfig.class})
class TaskControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private TaskService taskService;

  @Test
  void getTasksWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void getTasksWithUserRoleReturnsOk() throws Exception {
    Task task =
        new Task(
            UUID.randomUUID(),
            "Task 1",
            "Description",
            TaskStatus.TODO,
            Instant.now(),
            Instant.now());
    when(taskService.findAll()).thenReturn(List.of(task));

    mockMvc
        .perform(get("/api/tasks").with(jwt().authorities(role("user"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Task 1"));
  }

  @Test
  void getTasksWithoutUserRoleIsForbidden() throws Exception {
    mockMvc
        .perform(get("/api/tasks").with(jwt().authorities(role("admin"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void createTaskWithUserRoleReturnsCreated() throws Exception {
    TaskRequest request = new TaskRequest("New task", "Description", TaskStatus.TODO);
    Task created =
        new Task(
            UUID.randomUUID(),
            request.title(),
            request.description(),
            request.status(),
            Instant.now(),
            Instant.now());
    when(taskService.create(any(TaskRequest.class))).thenReturn(created);

    mockMvc
        .perform(
            post("/api/tasks")
                .with(jwt().authorities(role("user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("New task"));
  }

  @Test
  void createTaskWithBlankTitleIsBadRequest() throws Exception {
    TaskRequest request = new TaskRequest(" ", "Description", TaskStatus.TODO);

    mockMvc
        .perform(
            post("/api/tasks")
                .with(jwt().authorities(role("user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteTaskWithAdminRoleReturnsNoContent() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/tasks/{id}", id).with(jwt().authorities(role("admin"))))
        .andExpect(status().isNoContent());

    verify(taskService).delete(eq(id));
  }

  @Test
  void deleteTaskWithoutAdminRoleIsForbidden() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/tasks/{id}", id).with(jwt().authorities(role("user"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getMissingTaskReturnsProblemDetail() throws Exception {
    UUID id = UUID.randomUUID();
    when(taskService.findById(id)).thenThrow(new TaskNotFoundException(id));

    mockMvc
        .perform(get("/api/tasks/{id}", id).with(jwt().authorities(role("user"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Task not found"));
  }

  private static org.springframework.security.core.GrantedAuthority role(String role) {
    return new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role);
  }

  @TestConfiguration
  static class NoOpJwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder() {
      return mock(JwtDecoder.class);
    }
  }
}
