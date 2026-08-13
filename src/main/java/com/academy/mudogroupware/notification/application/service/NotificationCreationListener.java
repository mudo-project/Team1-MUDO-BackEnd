package com.academy.mudogroupware.notification.application.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;
import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.port.NotificationUserInfoPort;
import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// workspace/approval이 발행하는 원본 이벤트를 WorkspaceWebSocketNotifier/ApprovalWebSocketNotifier와
// 독립적으로 구독해 알림을 저장만 한다(팬아웃). 실시간 전송 실패와 저장 실패가 서로 영향을 주지 않는다.
// approval 패키지는 본인 담당이 아니라 ApprovalDocumentDecidedEvent를 기존 boolean 필드 그대로 소비한다.
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCreationListener {

    private static final String UNKNOWN_NAME = "알 수 없음";
    private static final String APPROVAL_DECIDED_WITHDRAWN_MESSAGE = "결재 문서 처리가 철회되었습니다.";

    private final CreateNotificationUseCase createNotificationUseCase;
    private final NotificationUserInfoPort notificationUserInfoPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TaskCommentMentionedEvent event) {
        Map<Long, String> nameByUserId = notificationUserInfoPort.findUserInfo(Set.of(event.actorUserId())).stream()
                .collect(Collectors.toMap(NotificationUserInfo::userId, NotificationUserInfo::name));
        String actorName = nameByUserId.getOrDefault(event.actorUserId(), UNKNOWN_NAME);

        event.recipientUserIds().forEach(recipientUserId -> create(
                recipientUserId,
                NotificationType.WORKSPACE_TASK_COMMENT_MENTION.name(),
                event.taskId(),
                actorName + "님이 댓글에서 회원님을 언급했습니다"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApprovalLineActivatedEvent event) {
        create(event.approverId(), NotificationType.APPROVAL_LINE_ACTIVATED.name(), event.documentId(),
                "결재 문서 [" + event.documentTitle() + "] 결재 차례가 되었습니다");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApprovalDocumentDecidedEvent event) {
        // approved=false는 반려/취소를 이벤트만으로 구분할 수 없어 중립적인 문구로 뭉뚱그린다.
        // 상태 enum 추가는 approval 담당 팀원에게 별도 요청함(docs/superpowers/specs/2026-08-13-notification-persistence-design.md 참고).
        String message = event.approved() ? "결재 문서가 승인되었습니다" : APPROVAL_DECIDED_WITHDRAWN_MESSAGE;
        create(event.requesterId(), NotificationType.APPROVAL_DOCUMENT_DECIDED.name(), event.documentId(), message);
    }

    // 실시간 전송(WorkspaceWebSocketNotifier/ApprovalWebSocketNotifier)에 영향을 주지 않도록
    // 저장 실패는 여기서 흡수하고 로그만 남긴다.
    private void create(Long recipientUserId, String type, Long targetId, String message) {
        try {
            createNotificationUseCase.create(new CreateNotificationCommand(recipientUserId, type, targetId, message));
        } catch (RuntimeException exception) {
            log.error("event=notification_persist_실패 recipientUserId={} type={} targetId={}",
                    recipientUserId, type, targetId, exception);
        }
    }
}
