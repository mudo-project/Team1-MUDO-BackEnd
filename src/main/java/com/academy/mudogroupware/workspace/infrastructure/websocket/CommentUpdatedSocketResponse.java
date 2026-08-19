package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.CommentUpdatedEvent;
import java.time.LocalDateTime;

public record CommentUpdatedSocketResponse(
    String eventType, Long workspaceId, Long taskId, Long commentId, String content, LocalDateTime updatedAt) {

  private static final String EVENT_TYPE = "COMMENT_UPDATED";

  public static CommentUpdatedSocketResponse from(CommentUpdatedEvent event) {
    return new CommentUpdatedSocketResponse(
        EVENT_TYPE, event.workspaceId(), event.taskId(), event.commentId(), event.content(), event.updatedAt());
  }
}
