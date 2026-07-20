package com.showcase.backend.task.dto;

import com.showcase.backend.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request payload for creating or updating a task. */
public record TaskRequest(
    @NotBlank @Size(max = 200) String title, String description, @NotNull TaskStatus status) {}
