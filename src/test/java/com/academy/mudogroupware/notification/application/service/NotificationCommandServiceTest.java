package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-13T01:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationCommandService service() {
        return new NotificationCommandService(notificationRepository, FIXED_CLOCK);
    }

    @Test
    void createSavesNotificationAndReturnsId() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture()))
                .thenAnswer(invocation -> Notification.restore(1L, 10L,
                        NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                        null, LocalDateTime.now(FIXED_CLOCK)));

        Long id = service().create(new CreateNotificationCommand(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다"));

        assertThat(id).isEqualTo(1L);
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getMessage()).isEqualTo("결재 차례가 되었습니다");
        verify(notificationRepository).save(captor.getValue());
    }

    @Test
    void markAsReadDelegatesToRepositoryWithCurrentTime() {
        service().markAsRead(1L, 10L);

        verify(notificationRepository).markAsRead(1L, 10L, LocalDateTime.now(FIXED_CLOCK));
    }

    @Test
    void deleteDelegatesToRepositoryWithCurrentTime() {
        service().delete(1L, 10L);

        verify(notificationRepository).delete(1L, 10L, LocalDateTime.now(FIXED_CLOCK));
    }

    @Test
    void deleteReadReturnsDeletedCountFromRepository() {
        when(notificationRepository.deleteAllReadByRecipientUserId(10L, LocalDateTime.now(FIXED_CLOCK)))
                .thenReturn(3);

        int deletedCount = service().deleteRead(10L);

        assertThat(deletedCount).isEqualTo(3);
    }
}
