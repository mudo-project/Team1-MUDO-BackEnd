package com.academy.mudogroupware.workspace.infrastructure.websocket;

import com.academy.mudogroupware.global.infrastructure.websocket.WebSocketEventPublisher;
import com.academy.mudogroupware.workspace.domain.event.TaskCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 업무·댓글 변경을 워크스페이스 단위로 브로드캐스트한다. 멘션 알림 전용인
// WorkspaceWebSocketNotifier와는 완전히 독립적으로 공존한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceRealtimeNotifier {

  private static final String WORKSPACE_TOPIC_PREFIX = "/topic/workspaces/";

  private final WebSocketEventPublisher eventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(TaskCreatedEvent event) {
    try {
      eventPublisher.publish(
          WORKSPACE_TOPIC_PREFIX + event.workspaceId(), TaskCreatedSocketResponse.from(event));
    } catch (RuntimeException exception) {
      log.error(
          "event=workspace_realtime_task_created_전송_실패 workspaceId={}, taskId={}",
          event.workspaceId(),
          event.taskId(),
          exception);
    }
  }
}
