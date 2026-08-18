package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.CommentDeletedEvent;

public record CommentDeletedSocketResponse(String eventType, Long workspaceId, Long taskId, Long commentId) {

  private static final String EVENT_TYPE = "COMMENT_DELETED";

  public static CommentDeletedSocketResponse from(CommentDeletedEvent event) {
    return new CommentDeletedSocketResponse(
        EVENT_TYPE, event.workspaceId(), event.taskId(), event.commentId());
  }
}
