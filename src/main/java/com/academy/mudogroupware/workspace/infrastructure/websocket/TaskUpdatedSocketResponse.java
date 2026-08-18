package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.TaskUpdatedEvent;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;

public record TaskUpdatedSocketResponse(
    String eventType, Long workspaceId, Long taskId, TaskStatus status, LocalDate dueAt) {

  private static final String EVENT_TYPE = "TASK_UPDATED";

  public static TaskUpdatedSocketResponse from(TaskUpdatedEvent event) {
    return new TaskUpdatedSocketResponse(
        EVENT_TYPE, event.workspaceId(), event.taskId(), event.status(), event.dueAt());
  }
}
