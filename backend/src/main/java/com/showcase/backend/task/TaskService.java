package com.showcase.backend.task;

import com.showcase.backend.exception.TaskNotFoundException;
import com.showcase.backend.task.dto.TaskRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional(readOnly = true)
  public List<Task> findAll() {
    return taskRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Task findById(UUID id) {
    return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
  }

  public Task create(TaskRequest request) {
    Task task = Task.createNew(request.title(), request.description());
    if (request.status() != null) {
      task.applyUpdate(request.title(), request.description(), request.status());
    }
    return taskRepository.save(task);
  }

  public Task update(UUID id, TaskRequest request) {
    Task task = findById(id);
    task.applyUpdate(request.title(), request.description(), request.status());
    return taskRepository.save(task);
  }

  public void delete(UUID id) {
    if (!taskRepository.existsById(id)) {
      throw new TaskNotFoundException(id);
    }
    taskRepository.deleteById(id);
  }
}
