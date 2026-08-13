package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.port.NotificationUserInfoPort;
import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
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
    }

    @Test
    void approvalDocumentApprovedEventCreatesApprovedMessage() {
        ApprovalDocumentDecidedEvent event = new ApprovalDocumentDecidedEvent(
                300L, 30L, true, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(30L);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.APPROVAL_DOCUMENT_DECIDED.name());
        assertThat(captor.getValue().message()).isEqualTo("결재 문서가 승인되었습니다");
    }

    @Test
    void approvalDocumentNotApprovedEventCreatesNeutralWithdrawnMessage() {
        // 반려(REJECTED)와 취소(CANCELLED)가 이벤트 상에서 모두 approved=false로 와서 구분할 수 없으므로
        // 중립적인 문구로 뭉뚱그린다. 정확한 구분은 approval 담당 팀원에게 별도 요청함.
        ApprovalDocumentDecidedEvent event = new ApprovalDocumentDecidedEvent(
                300L, 30L, false, LocalDateTime.of(2026, 8, 13, 10, 0));

        listener().handle(event);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().message()).isEqualTo("결재 문서 처리가 철회되었습니다.");
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
