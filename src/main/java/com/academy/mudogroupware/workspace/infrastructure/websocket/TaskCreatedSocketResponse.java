package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.TaskCreatedEvent;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskCreatedSocketResponse(
    String eventType,
    Long workspaceId,
    Long taskId,
    String title,
    TaskStatus status,
    LocalDate dueAt,
    Long createdBy,
    LocalDateTime createdAt) {

  private static final String EVENT_TYPE = "TASK_CREATED";

  public static TaskCreatedSocketResponse from(TaskCreatedEvent event) {
    return new TaskCreatedSocketResponse(
        EVENT_TYPE,
        event.workspaceId(),
        event.taskId(),
        event.title(),
        event.status(),
        event.dueAt(),
        event.createdBy(),
        event.createdAt());
  }
}
