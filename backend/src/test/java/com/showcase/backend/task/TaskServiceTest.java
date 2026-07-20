package com.showcase.backend.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.showcase.backend.exception.TaskNotFoundException;
import com.showcase.backend.task.dto.TaskRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;

  private TaskService taskService;

  @BeforeEach
  void setUp() {
    taskService = new TaskService(taskRepository);
  }

  @Test
  void findAllReturnsAllTasksFromRepository() {
    Task task = Task.createNew("Write tests", "Cover the service layer");
    when(taskRepository.findAll()).thenReturn(List.of(task));

    List<Task> result = taskService.findAll();

    assertThat(result).containsExactly(task);
  }

  @Test
  void findByIdReturnsTaskWhenPresent() {
    Task task = Task.createNew("Write tests", "Cover the service layer");
    when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

    Task result = taskService.findById(task.getId());

    assertThat(result).isEqualTo(task);
  }

  @Test
  void findByIdThrowsWhenMissing() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.findById(id))
        .isInstanceOf(TaskNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void createPersistsNewTaskWithRequestedStatus() {
    TaskRequest request = new TaskRequest("New task", "Description", TaskStatus.IN_PROGRESS);
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task result = taskService.create(request);

    assertThat(result.getTitle()).isEqualTo("New task");
    assertThat(result.getDescription()).isEqualTo("Description");
    assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void updateModifiesExistingTask() {
    Task existing =
        new Task(
            UUID.randomUUID(),
            "Old title",
            "Old description",
            TaskStatus.TODO,
            Instant.now(),
            Instant.now());
    when(taskRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TaskRequest request = new TaskRequest("New title", "New description", TaskStatus.DONE);
    Task result = taskService.update(existing.getId(), request);

    assertThat(result.getTitle()).isEqualTo("New title");
    assertThat(result.getDescription()).isEqualTo("New description");
    assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
  }

  @Test
  void updateThrowsWhenTaskMissing() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findById(id)).thenReturn(Optional.empty());

    TaskRequest request = new TaskRequest("Title", null, TaskStatus.TODO);

    assertThatThrownBy(() -> taskService.update(id, request))
        .isInstanceOf(TaskNotFoundException.class);
  }

  @Test
  void deleteRemovesExistingTask() {
    UUID id = UUID.randomUUID();
    when(taskRepository.existsById(id)).thenReturn(true);

    taskService.delete(id);

    verify(taskRepository).deleteById(id);
  }

  @Test
  void deleteThrowsWhenTaskMissing() {
    UUID id = UUID.randomUUID();
    when(taskRepository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> taskService.delete(id)).isInstanceOf(TaskNotFoundException.class);
  }
}
