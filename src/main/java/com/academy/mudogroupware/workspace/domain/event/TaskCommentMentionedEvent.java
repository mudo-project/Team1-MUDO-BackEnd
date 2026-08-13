package com.academy.mudogroupware.workspace.domain.event;

import java.time.LocalDateTime;
import java.util.List;

// 멘션 알림 이벤트
public record TaskCommentMentionedEvent(
    Long workspaceId,
    Long taskId,
    String taskTitle,
    Long commentId,
    Long actorUserId,
    List<Long> recipientUserIds, // 알림을 받을 사용자 목록
    LocalDateTime occurredAt) {

  public TaskCommentMentionedEvent {
    recipientUserIds = List.copyOf(recipientUserIds);
  }
}
