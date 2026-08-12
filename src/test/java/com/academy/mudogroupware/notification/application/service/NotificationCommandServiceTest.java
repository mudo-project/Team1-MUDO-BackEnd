package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationCommandService service() {
        return new NotificationCommandService(notificationRepository);
    }

    @Test
    void createSavesNotificationAndReturnsId() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture()))
                .thenAnswer(invocation -> Notification.restore(1L, 10L,
                        NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                        null, java.time.LocalDateTime.now()));

        Long id = service().create(new CreateNotificationCommand(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다"));

        assertThat(id).isEqualTo(1L);
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getMessage()).isEqualTo("결재 차례가 되었습니다");
        verify(notificationRepository).save(captor.getValue());
    }
}
