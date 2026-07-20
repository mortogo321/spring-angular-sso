package com.showcase.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.showcase.backend.task.Task;
import com.showcase.backend.task.TaskRepository;
import com.showcase.backend.task.TaskService;
import com.showcase.backend.task.TaskStatus;
import com.showcase.backend.task.dto.TaskRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BackendApplicationIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private TaskRepository taskRepository;

  @Autowired private TaskService taskService;

  @Test
  void contextLoads() {
    assertThat(taskRepository).isNotNull();
  }

  @Test
  void flywaySeedDataIsPresent() {
    assertThat(taskRepository.count()).isGreaterThanOrEqualTo(3);
  }

  @Test
  void fullCrudLifecyclePersistsAgainstRealPostgres() {
    TaskRequest createRequest =
        new TaskRequest("Integration task", "Created by the integration test", TaskStatus.TODO);
    Task created = taskService.create(createRequest);
    assertThat(created.getId()).isNotNull();

    Task fetched = taskService.findById(created.getId());
    assertThat(fetched.getTitle()).isEqualTo("Integration task");

    TaskRequest updateRequest = new TaskRequest("Integration task", "Updated", TaskStatus.DONE);
    Task updated = taskService.update(created.getId(), updateRequest);
    assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
    assertThat(updated.getDescription()).isEqualTo("Updated");

    UUID id = created.getId();
    taskService.delete(id);
    assertThat(taskRepository.findById(id)).isEmpty();
  }

  @TestConfiguration
  static class NoOpJwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder() {
      return mock(JwtDecoder.class);
    }
  }
}
