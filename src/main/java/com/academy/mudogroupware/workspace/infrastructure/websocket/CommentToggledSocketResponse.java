package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.CommentToggledEvent;
import java.time.LocalDateTime;

public record CommentToggledSocketResponse(
    String eventType,
    Long workspaceId,
    Long taskId,
    Long commentId,
    boolean completed,
    Long completedBy,
    LocalDateTime completedAt) {

  private static final String EVENT_TYPE = "COMMENT_TOGGLED";

  public static CommentToggledSocketResponse from(CommentToggledEvent event) {
    return new CommentToggledSocketResponse(
        EVENT_TYPE,
        event.workspaceId(),
        event.taskId(),
        event.commentId(),
        event.completed(),
        event.completedBy(),
        event.completedAt());
  }
}
