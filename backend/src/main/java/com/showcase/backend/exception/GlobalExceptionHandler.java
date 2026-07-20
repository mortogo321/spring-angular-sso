package com.showcase.backend.exception;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Central exception handling producing RFC 9457 {@link ProblemDetail} responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final URI NOT_FOUND_TYPE =
      URI.create("https://showcase.example/problems/task-not-found");
  private static final URI VALIDATION_TYPE =
      URI.create("https://showcase.example/problems/validation-error");

  @ExceptionHandler(TaskNotFoundException.class)
  public ProblemDetail handleTaskNotFound(TaskNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Task not found");
    problem.setType(NOT_FOUND_TYPE);
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setTitle("Validation error");
    problem.setType(VALIDATION_TYPE);
    problem.setProperty("timestamp", Instant.now());
    problem.setProperty(
        "errors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getField)
            .distinct()
            .toList());
    return problem;
  }
}
