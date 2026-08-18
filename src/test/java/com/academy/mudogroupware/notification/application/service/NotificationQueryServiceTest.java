package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    private static final long RECIPIENT_ID = 10L;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationQueryService service() {
        return new NotificationQueryService(notificationRepository);
    }

    @Test
    void returnsNotificationsForRecipient() {
        Notification notification = Notification.restore(1L, RECIPIENT_ID,
                NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE_ACTIVATED:100:10", null, java.time.LocalDateTime.of(2026, 8, 13, 9, 0));
        when(notificationRepository.findAllByRecipientUserId(RECIPIENT_ID, 0, 20))
                .thenReturn(PageResult.of(List.of(notification), 0, 20, false));

        PageResult<Notification> result = service().getNotifications(RECIPIENT_ID, 0, 20);

        assertThat(result.content()).containsExactly(notification);
    }

    @Test
    void returnsUnreadCount() {
        when(notificationRepository.countUnreadByRecipientUserId(RECIPIENT_ID)).thenReturn(3L);

        long count = service().countUnread(RECIPIENT_ID);

        assertThat(count).isEqualTo(3L);
    }
}
