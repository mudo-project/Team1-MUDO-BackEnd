package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WorkspaceWebSocketNotifier {

  private static final String USER_TOPIC_PREFIX = "/topic/workspaces/users/";

  private final WebSocketEventPublisher eventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(TaskCommentMentionedEvent event) {
    event.recipientUserIds().forEach(
        recipientUserId ->
            eventPublisher.publish(
                USER_TOPIC_PREFIX + recipientUserId,
                TaskCommentMentionedSocketResponse.from(event, recipientUserId)));
  }
}
