package com.academy.mudogroupware.workspace.domain.event;

import java.time.LocalDateTime;
import java.util.List;

public record TaskCommentMentionedEvent(
    Long workspaceId,
    Long taskId,
    String taskTitle,
    Long commentId,
    Long actorUserId,
    List<Long> recipientUserIds,
    LocalDateTime occurredAt) {

  public TaskCommentMentionedEvent {
    recipientUserIds = List.copyOf(recipientUserIds);
  }
}
