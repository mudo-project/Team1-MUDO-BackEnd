package com.academy.mudogroupware.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void createdNotificationIsUnread() {
        Notification notification = Notification.create(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE:100:10");

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @Test
    void markAsReadSetsReadAt() {
        Notification notification = Notification.restore(
                1L, 10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE:100:10", null, LocalDateTime.of(2026, 8, 13, 9, 0));

        notification.markAsRead(LocalDateTime.of(2026, 8, 13, 9, 30));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(LocalDateTime.of(2026, 8, 13, 9, 30));
    }

    @Test
    void markAsReadIsIdempotent() {
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 8, 13, 9, 30);
        Notification notification = Notification.restore(
                1L, 10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE:100:10", firstReadAt, LocalDateTime.of(2026, 8, 13, 9, 0));

        notification.markAsRead(LocalDateTime.of(2026, 8, 13, 10, 0));

        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    void markAsReadRejectsNull() {
        Notification notification = Notification.create(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE:100:10");

        assertThatThrownBy(() -> notification.markAsRead(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsMessageLongerThan250Characters() {
        String tooLong = "가".repeat(251);

        assertThatThrownBy(() -> Notification.create(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, tooLong,
                "APPROVAL_LINE:100:10"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createAcceptsMessageExactly250Characters() {
        String exactly250 = "가".repeat(250);

        Notification notification = Notification.create(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, exactly250,
                "APPROVAL_LINE:100:10");

        assertThat(notification.getMessage()).hasSize(250);
    }

    @Test
    void createExposesIdempotencyKey() {
        Notification notification = Notification.create(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE:100:10");

        assertThat(notification.getIdempotencyKey()).isEqualTo("APPROVAL_LINE:100:10");
    }

    @Test
    void createRejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> Notification.create(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
