package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.CommentCreatedEvent;
import java.time.LocalDateTime;

public record CommentCreatedSocketResponse(
    String eventType,
    Long workspaceId,
    Long taskId,
    Long commentId,
    Long authorId,
    String content,
    LocalDateTime createdAt) {

  private static final String EVENT_TYPE = "COMMENT_CREATED";

  public static CommentCreatedSocketResponse from(CommentCreatedEvent event) {
    return new CommentCreatedSocketResponse(
        EVENT_TYPE,
        event.workspaceId(),
        event.taskId(),
        event.commentId(),
        event.authorId(),
        event.content(),
        event.createdAt());
  }
}
