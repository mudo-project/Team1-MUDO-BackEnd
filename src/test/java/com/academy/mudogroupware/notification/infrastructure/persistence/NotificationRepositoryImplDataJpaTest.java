package com.academy.mudogroupware.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.notification.domain.exception.NotificationException;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.model.NotificationType;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, NotificationRepositoryImpl.class})
class NotificationRepositoryImplDataJpaTest {

    private static final long RECIPIENT_ID = 10L;

    @Autowired
    private NotificationRepositoryImpl notificationRepository;

    @Test
    void savesAndListsNewestFirst() {
        notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 1L, "첫 번째"));
        notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 2L, "두 번째"));

        PageResult<Notification> result = notificationRepository.findAllByRecipientUserId(RECIPIENT_ID, 0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).getMessage()).isEqualTo("두 번째");
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void countsOnlyUnread() {
        Notification saved = notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 1L, "안읽음"));
        notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 2L, "읽음 처리될 것"));
        notificationRepository.markAsRead(saved.getId(), RECIPIENT_ID, LocalDateTime.now());
        // save()가 반환한 두 번째 알림은 읽음 처리하지 않아 안읽음 1건만 남는다.

        long unreadCount = notificationRepository.countUnreadByRecipientUserId(RECIPIENT_ID);

        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void deleteOfOthersNotificationThrowsNotFound() {
        Notification saved = notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 1L, "본인 알림"));

        assertThatThrownBy(() -> notificationRepository.delete(saved.getId(), 999L, LocalDateTime.now()))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void deleteAllReadRemovesOnlyReadOnes() {
        Notification unread = notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 1L, "안읽음"));
        Notification read = notificationRepository.save(Notification.create(
                RECIPIENT_ID, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 2L, "읽음"));
        notificationRepository.markAsRead(read.getId(), RECIPIENT_ID, LocalDateTime.now());

        int deletedCount = notificationRepository.deleteAllReadByRecipientUserId(RECIPIENT_ID, LocalDateTime.now());

        assertThat(deletedCount).isEqualTo(1);
        assertThat(notificationRepository.findAllByRecipientUserId(RECIPIENT_ID, 0, 20).content())
                .extracting(Notification::getId)
                .containsExactly(unread.getId());
    }
}
