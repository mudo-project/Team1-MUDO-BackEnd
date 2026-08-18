package com.academy.mudogroupware.workspace.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;
import com.academy.mudogroupware.workspace.domain.event.CommentCreatedEvent;
import com.academy.mudogroupware.workspace.domain.event.CommentUpdatedEvent;
import com.academy.mudogroupware.workspace.domain.event.TaskCreatedEvent;
import com.academy.mudogroupware.workspace.domain.event.TaskDeletedEvent;
import com.academy.mudogroupware.workspace.domain.event.TaskUpdatedEvent;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WorkspaceRealtimeNotifierTest {

  private final WebSocketEventPublisher eventPublisher = mock(WebSocketEventPublisher.class);
  private final WorkspaceRealtimeNotifier notifier = new WorkspaceRealtimeNotifier(eventPublisher);

  @Test
  void publishesTaskCreatedEventToWorkspaceTopic() {
    TaskCreatedEvent event =
        new TaskCreatedEvent(
            2L, 501L, "새 업무", TaskStatus.WAITING, LocalDate.of(2026, 9, 1), 10L,
            LocalDateTime.of(2026, 8, 18, 10, 0));
    ArgumentCaptor<TaskCreatedSocketResponse> payloadCaptor =
        ArgumentCaptor.forClass(TaskCreatedSocketResponse.class);

    notifier.handle(event);

    verify(eventPublisher).publish(eq("/topic/workspaces/2"), payloadCaptor.capture());
    TaskCreatedSocketResponse payload = payloadCaptor.getValue();
    assertThat(payload.eventType()).isEqualTo("TASK_CREATED");
    assertThat(payload.taskId()).isEqualTo(501L);
    assertThat(payload.title()).isEqualTo("새 업무");
  }

  @Test
  void doesNotPropagateExceptionWhenPublishFails() {
    TaskCreatedEvent event =
        new TaskCreatedEvent(
            2L, 501L, "새 업무", TaskStatus.WAITING, LocalDate.of(2026, 9, 1), 10L,
            LocalDateTime.of(2026, 8, 18, 10, 0));
    doThrow(new RuntimeException("boom")).when(eventPublisher).publish(eq("/topic/workspaces/2"), any());

    notifier.handle(event); // 예외가 여기서 전파되면 테스트 자체가 실패한다
  }

  @Test
  void publishesTaskUpdatedEventToWorkspaceTopic() {
    TaskUpdatedEvent event = new TaskUpdatedEvent(2L, 501L, TaskStatus.COMPLETED, null);
    ArgumentCaptor<TaskUpdatedSocketResponse> payloadCaptor =
        ArgumentCaptor.forClass(TaskUpdatedSocketResponse.class);

    notifier.handle(event);

    verify(eventPublisher).publish(eq("/topic/workspaces/2"), payloadCaptor.capture());
    TaskUpdatedSocketResponse payload = payloadCaptor.getValue();
    assertThat(payload.eventType()).isEqualTo("TASK_UPDATED");
    assertThat(payload.status()).isEqualTo(TaskStatus.COMPLETED);
  }

  @Test
  void publishesTaskDeletedEventToWorkspaceTopic() {
    TaskDeletedEvent event = new TaskDeletedEvent(2L, 501L);
    ArgumentCaptor<TaskDeletedSocketResponse> payloadCaptor =
        ArgumentCaptor.forClass(TaskDeletedSocketResponse.class);

    notifier.handle(event);

    verify(eventPublisher).publish(eq("/topic/workspaces/2"), payloadCaptor.capture());
    assertThat(payloadCaptor.getValue().eventType()).isEqualTo("TASK_DELETED");
    assertThat(payloadCaptor.getValue().taskId()).isEqualTo(501L);
  }

  @Test
  void publishesCommentCreatedEventToWorkspaceTopic() {
    CommentCreatedEvent event =
        new CommentCreatedEvent(2L, 501L, 88L, 10L, "댓글 내용", LocalDateTime.of(2026, 8, 18, 11, 0));
    ArgumentCaptor<CommentCreatedSocketResponse> payloadCaptor =
        ArgumentCaptor.forClass(CommentCreatedSocketResponse.class);

    notifier.handle(event);

    verify(eventPublisher).publish(eq("/topic/workspaces/2"), payloadCaptor.capture());
    assertThat(payloadCaptor.getValue().eventType()).isEqualTo("COMMENT_CREATED");
    assertThat(payloadCaptor.getValue().commentId()).isEqualTo(88L);
  }

  @Test
  void publishesCommentUpdatedEventToWorkspaceTopic() {
    CommentUpdatedEvent event =
        new CommentUpdatedEvent(2L, 501L, 88L, "수정된 내용", LocalDateTime.of(2026, 8, 18, 12, 0));
    ArgumentCaptor<CommentUpdatedSocketResponse> payloadCaptor =
        ArgumentCaptor.forClass(CommentUpdatedSocketResponse.class);

    notifier.handle(event);

    verify(eventPublisher).publish(eq("/topic/workspaces/2"), payloadCaptor.capture());
    assertThat(payloadCaptor.getValue().eventType()).isEqualTo("COMMENT_UPDATED");
    assertThat(payloadCaptor.getValue().content()).isEqualTo("수정된 내용");
  }
}
