package com.academy.mudogroupware.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.model.NotificationType;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

// @Retryable가 실제로 동작하려면 Spring AOP 프록시를 통해야 하므로, new NotificationCommandService(...)로
// 직접 생성하는 순수 단위 테스트(NotificationCommandServiceTest)가 아니라 스프링 컨텍스트를 띄운다.
@SpringJUnitConfig
@ContextConfiguration(classes = NotificationCommandServiceRetryTest.Config.class)
class NotificationCommandServiceRetryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("UTC"));

    @Autowired
    private CreateNotificationUseCase notificationCommandService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void resetMock() {
        // Spring 컨텍스트가 테스트 메서드 간에 캐시되어 mock이 공유되므로, 이전 테스트의 스텁이
        // 남아있지 않게 매번 리셋한다.
        Mockito.reset(notificationRepository);
    }

    @Test
    void retriesOnTransientFailureAndEventuallySucceeds() {
        Notification saved = Notification.restore(1L, 10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(),
                100L, "결재 차례가 되었습니다", "APPROVAL_LINE_ACTIVATED:100:10", null, LocalDateTime.now(FIXED_CLOCK));
        when(notificationRepository.save(any()))
                .thenThrow(new TransientDataAccessResourceException("일시적 커넥션 오류"))
                .thenThrow(new TransientDataAccessResourceException("일시적 커넥션 오류"))
                .thenReturn(saved);

        Long id = notificationCommandService.create(new CreateNotificationCommand(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE_ACTIVATED:100:10"));

        assertThat(id).isEqualTo(1L);
        verify(notificationRepository, times(3)).save(any());
    }

    @Test
    void duplicateIdempotencyKeyIsIgnoredWithoutRetry() {
        when(notificationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_notification_idempotency_key 위반"));

        Long id = notificationCommandService.create(new CreateNotificationCommand(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE_ACTIVATED:100:10"));

        assertThat(id).isNull();
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void nonIdempotencyConstraintViolationIsNotSwallowed() {
        // fk_notification_recipient 같은 다른 제약 위반은 "이미 저장된 알림"이 아니라 진짜 버그다.
        // 조용히 무시하면 안 되고 그대로 다시 던져져야 한다.
        when(notificationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails "
                                + "(`fk_notification_recipient`)"));

        assertThatThrownBy(() -> notificationCommandService.create(new CreateNotificationCommand(
                10L, NotificationType.APPROVAL_LINE_ACTIVATED.name(), 100L, "결재 차례가 되었습니다",
                "APPROVAL_LINE_ACTIVATED:100:10")))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(notificationRepository, times(1)).save(any());
    }

    @TestConfiguration
    @EnableRetry
    static class Config {

        @Bean
        NotificationRepository notificationRepository() {
            return mock(NotificationRepository.class);
        }

        @Bean
        Clock clock() {
            return FIXED_CLOCK;
        }

        @Bean
        NotificationCommandService notificationCommandService(
                NotificationRepository notificationRepository, Clock clock) {
            return new NotificationCommandService(notificationRepository, clock);
        }
    }
}
