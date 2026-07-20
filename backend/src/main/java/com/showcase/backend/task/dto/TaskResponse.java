package com.showcase.backend.task.dto;

import com.showcase.backend.task.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    Instant createdAt,
    Instant updatedAt) {}
