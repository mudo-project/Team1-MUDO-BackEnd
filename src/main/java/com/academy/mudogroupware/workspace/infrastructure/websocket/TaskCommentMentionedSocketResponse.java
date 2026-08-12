package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import java.time.LocalDateTime;

public record TaskCommentMentionedSocketResponse(
    String eventType,
    Long workspaceId,
    Long taskId,
    String taskTitle,
    Long commentId,
    Long actorUserId,
    Long recipientUserId, // 이벤트를 받는 사용자
    LocalDateTime occurredAt) {

  // 이벤트 타입
  private static final String EVENT_TYPE = "TASK_COMMENT_MENTIONED";

  public static TaskCommentMentionedSocketResponse from(
      TaskCommentMentionedEvent event, Long recipientUserId) {
    return new TaskCommentMentionedSocketResponse(
        EVENT_TYPE,
        event.workspaceId(),
        event.taskId(),
        event.taskTitle(),
        event.commentId(),
        event.actorUserId(),
        recipientUserId,
        event.occurredAt());
  }
}
