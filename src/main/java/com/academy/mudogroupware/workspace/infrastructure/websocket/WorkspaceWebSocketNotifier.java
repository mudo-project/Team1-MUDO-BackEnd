package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceWebSocketNotifier {

  private static final String USER_TOPIC_PREFIX = "/topic/workspaces/users/";

  private final WebSocketEventPublisher eventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(TaskCommentMentionedEvent event) {
    event.recipientUserIds().forEach(recipientUserId -> publish(event, recipientUserId));
  }

  private void publish(TaskCommentMentionedEvent event, Long recipientUserId) {
    try {
      eventPublisher.publish(
          USER_TOPIC_PREFIX + recipientUserId,
          TaskCommentMentionedSocketResponse.from(event, recipientUserId));
    } catch (RuntimeException exception) {
      log.error(
          "event=task_comment_mention_notification_실패 workspaceId={}, taskId={}, commentId={}, recipientUserId={}",
          event.workspaceId(),
          event.taskId(),
          event.commentId(),
          recipientUserId,
          exception);
    }
  }
}
