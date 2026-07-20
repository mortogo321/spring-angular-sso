package com.showcase.backend.task.dto;

import com.showcase.backend.task.Task;

/**
 * Manual mapping between the {@link Task} entity and its DTOs. Kept intentionally simple for this
 * showcase - a mapping framework (e.g. MapStruct) would be overkill for a single entity with this
 * few fields.
 */
public final class TaskMapper {

  private TaskMapper() {}

  public static TaskResponse toResponse(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
