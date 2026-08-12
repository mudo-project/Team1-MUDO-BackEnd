package com.academy.mudogroupware.workspace.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class WorkspaceWebSocketNotifierTest {

  private final WebSocketEventPublisher eventPublisher = mock(WebSocketEventPublisher.class);
  private final WorkspaceWebSocketNotifier notifier = new WorkspaceWebSocketNotifier(eventPublisher);

  @Test
  void sendsMentionEventToEachRecipientTopicInOrder() {
    LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 12, 10, 30);
    TaskCommentMentionedEvent event =
        new TaskCommentMentionedEvent(
            1L, 101L, "상담 일지 작성", 501L, 10L, List.of(11L, 12L), occurredAt);
    ArgumentCaptor<TaskCommentMentionedSocketResponse> payloadCaptor =
        ArgumentCaptor.forClass(TaskCommentMentionedSocketResponse.class);
    InOrder orderedPublisher = inOrder(eventPublisher);

    notifier.handle(event);

    orderedPublisher
        .verify(eventPublisher)
        .publish(eq("/topic/workspaces/users/11"), payloadCaptor.capture());
    orderedPublisher
        .verify(eventPublisher)
        .publish(eq("/topic/workspaces/users/12"), payloadCaptor.capture());
    orderedPublisher.verifyNoMoreInteractions();

    assertThat(payloadCaptor.getAllValues())
        .containsExactly(
            new TaskCommentMentionedSocketResponse(
                "TASK_COMMENT_MENTIONED", 1L, 101L, "상담 일지 작성", 501L, 10L, 11L,
                occurredAt),
            new TaskCommentMentionedSocketResponse(
                "TASK_COMMENT_MENTIONED", 1L, 101L, "상담 일지 작성", 501L, 10L, 12L,
                occurredAt));
  }

  @Test
  void continuesWithNextRecipientWhenOnePublishFails() {
    TaskCommentMentionedEvent event =
        new TaskCommentMentionedEvent(
            1L,
            101L,
            "상담 일지 작성",
            501L,
            10L,
            List.of(11L, 12L),
            LocalDateTime.of(2026, 8, 12, 10, 30));
    doThrow(new IllegalStateException("broker unavailable"))
        .when(eventPublisher)
        .publish(eq("/topic/workspaces/users/11"), any(TaskCommentMentionedSocketResponse.class));

    notifier.handle(event);

    verify(eventPublisher)
        .publish(eq("/topic/workspaces/users/12"), any(TaskCommentMentionedSocketResponse.class));
  }
}
