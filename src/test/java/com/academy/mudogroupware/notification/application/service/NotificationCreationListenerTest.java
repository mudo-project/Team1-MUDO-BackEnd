package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.port.NotificationUserInfoPort;
import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
import com.academy.mudogroupware.revenuereport.domain.event.RevenueReportGeneratedEvent;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;

@ExtendWith(MockitoExtension.class)
class NotificationCreationListenerTest {

    @Mock
    private CreateNotificationUseCase createNotificationUseCase;

    @Mock
    private NotificationUserInfoPort notificationUserInfoPort;

    private NotificationCreationListener listener() {
        return new NotificationCreationListener(createNotificationUseCase, notificationUserInfoPort);
    }

    @Test
    void mentionEventCreatesOneNotificationPerRecipientWithActorName() {
        when(notificationUserInfoPort.findUserInfo(Set.of(10L)))
                .thenReturn(List.of(new NotificationUserInfo(10L, "윤예진")));
        TaskCommentMentionedEvent event = new TaskCommentMentionedEvent(
                1L, 101L, "상담 일지 작성", 501L, 10L, List.of(11L, 12L),
                LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase, org.mockito.Mockito.times(2)).create(captor.capture());
        assertThat(captor.getAllValues()).extracting(CreateNotificationCommand::recipientUserId)
                .containsExactly(11L, 12L);
        assertThat(captor.getAllValues()).allSatisfy(command -> {
            assertThat(command.type()).isEqualTo(NotificationType.WORKSPACE_TASK_COMMENT_MENTION.name());
            assertThat(command.targetId()).isEqualTo(101L);
            assertThat(command.message()).isEqualTo("윤예진님이 댓글에서 회원님을 언급했습니다");
        });
        // 멱등키는 target_id(taskId)가 아니라 commentId+recipientUserId 기준이라, 같은 업무의
        // 다른 댓글에서 같은 사람이 또 멘션돼도 서로 다른 키가 나와야 한다.
        assertThat(captor.getAllValues()).extracting(CreateNotificationCommand::idempotencyKey)
                .containsExactly(
                        "WORKSPACE_TASK_COMMENT_MENTION:501:11",
                        "WORKSPACE_TASK_COMMENT_MENTION:501:12");
    }

    @Test
    void approvalLineActivatedEventCreatesNotificationForNextApprover() {
        ApprovalLineActivatedEvent event = new ApprovalLineActivatedEvent(
                200L, "휴가 신청서", 20L, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(20L);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.APPROVAL_LINE_ACTIVATED.name());
        assertThat(captor.getValue().targetId()).isEqualTo(200L);
        assertThat(captor.getValue().message()).isEqualTo("결재 문서 [휴가 신청서] 결재 차례가 되었습니다");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("APPROVAL_LINE_ACTIVATED:200:20");
    }

    @Test
    void approvalDocumentApprovedEventCreatesApprovedMessage() {
        ApprovalDocumentDecidedEvent event = new ApprovalDocumentDecidedEvent(
                300L, 30L, ApprovalStatus.APPROVED, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(30L);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.APPROVAL_DOCUMENT_DECIDED.name());
        assertThat(captor.getValue().message()).isEqualTo("결재 문서가 승인되었습니다");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("APPROVAL_DOCUMENT_DECIDED:300:30");
    }

    @Test
    void approvalDocumentRejectedEventCreatesRejectedMessage() {
        ApprovalDocumentDecidedEvent event = new ApprovalDocumentDecidedEvent(
                300L, 30L, ApprovalStatus.REJECTED, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().message()).isEqualTo("결재 문서가 반려되었습니다");
    }

    @Test
    void approvalDocumentCancelledEventCreatesCancelledMessage() {
        ApprovalDocumentDecidedEvent event = new ApprovalDocumentDecidedEvent(
                300L, 30L, ApprovalStatus.CANCELLED, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().message()).isEqualTo("결재 문서가 취소되었습니다");
    }

    @Test
    void revenueReportGeneratedEventCreatesNotificationForRecipient() {
        RevenueReportGeneratedEvent event = new RevenueReportGeneratedEvent(7L, 99L, LocalDate.of(2026, 8, 1));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(7L);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.REVENUE_REPORT_GENERATED.name());
        assertThat(captor.getValue().targetId()).isEqualTo(99L);
        assertThat(captor.getValue().message()).isEqualTo("2026년 8월 매출 리포트가 생성되었습니다");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("REVENUE_REPORT_GENERATED:99:7");
    }

    @Test
    void exceptionDuringCreateIsSwallowed() {
        doThrow(new IllegalStateException("db down")).when(createNotificationUseCase).create(any());
        ApprovalLineActivatedEvent event = new ApprovalLineActivatedEvent(
                200L, "휴가 신청서", 20L, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        verify(createNotificationUseCase).create(any());
        // 예외가 던져지지 않고 여기까지 도달하면 흡수된 것이다.
    }
}
