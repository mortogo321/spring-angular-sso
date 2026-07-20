package com.showcase.backend.task;

import com.showcase.backend.task.dto.TaskMapper;
import com.showcase.backend.task.dto.TaskRequest;
import com.showcase.backend.task.dto.TaskResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public List<TaskResponse> findAll() {
    return taskService.findAll().stream().map(TaskMapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  public TaskResponse findById(@PathVariable UUID id) {
    return TaskMapper.toResponse(taskService.findById(id));
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
    Task created = taskService.create(request);
    TaskResponse response = TaskMapper.toResponse(created);
    return ResponseEntity.created(URI.create("/api/tasks/" + created.getId())).body(response);
  }

  @PutMapping("/{id}")
  public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
    return TaskMapper.toResponse(taskService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    taskService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
