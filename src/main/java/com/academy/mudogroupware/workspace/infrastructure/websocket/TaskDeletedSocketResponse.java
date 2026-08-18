package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.TaskDeletedEvent;

public record TaskDeletedSocketResponse(String eventType, Long workspaceId, Long taskId) {

  private static final String EVENT_TYPE = "TASK_DELETED";

  public static TaskDeletedSocketResponse from(TaskDeletedEvent event) {
    return new TaskDeletedSocketResponse(EVENT_TYPE, event.workspaceId(), event.taskId());
  }
}
