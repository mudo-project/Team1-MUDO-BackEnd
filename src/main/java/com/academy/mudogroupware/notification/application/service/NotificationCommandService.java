package com.academy.mudogroupware.notification.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.application.usecase.DeleteNotificationUseCase;
import com.academy.mudogroupware.notification.application.usecase.DeleteReadNotificationsUseCase;
import com.academy.mudogroupware.notification.application.usecase.MarkNotificationAsReadUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService implements CreateNotificationUseCase, MarkNotificationAsReadUseCase,
        DeleteNotificationUseCase, DeleteReadNotificationsUseCase {

    private static final String IDEMPOTENCY_KEY_CONSTRAINT_MARKER = "idempotency_key";

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    // 일시적 DB 오류(커넥션 끊김 등)만 재시도한다. 멱등키 유니크 제약 위반(DataIntegrityViolationException)은
    // TransientDataAccessException 계층에 속하지 않아 재시도 대상에서 자동으로 제외된다 —
    // 재시도해도 항상 똑같이 실패하는 결정론적 오류라서 재시도가 무의미하다.
    @Override
    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttempts = 3, backoff = @Backoff(delay = 200))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long create(CreateNotificationCommand command) {
        Notification notification = Notification.create(
                command.recipientUserId(),
                command.type(),
                command.targetId(),
                command.message(),
                command.idempotencyKey()
        );
        try {
            return notificationRepository.save(notification).getId();
        } catch (DataIntegrityViolationException exception) {
            if (!isIdempotencyKeyViolation(exception)) {
                // fk_notification_recipient 같은 다른 제약 위반은 "이미 저장됨"이 아니라 진짜 결함이다.
                // 멱등키 위반이 아니면 삼키지 않고 그대로 다시 던진다.
                throw exception;
            }
            // 이미 저장된 알림과 멱등키가 같다 = 같은 알림이 재시도/재발행으로 다시 들어온 것.
            // 새로 저장할 필요가 없으므로 성공으로 간주하고 넘어간다.
            log.info("event=notification_create_멱등_무시 idempotencyKey={}", command.idempotencyKey());
            return null;
        }
    }

    // 위반된 제약 이름을 취득해서(가능하면) 확인하고, DB/드라이버에 따라 이름이 못 붙거나 다르게 나올 수 있어
    // 예외 메시지에 "idempotency_key"가 들어있는지도 함께 확인한다.
    private boolean isIdempotencyKeyViolation(DataIntegrityViolationException exception) {
        Throwable mostSpecificCause = exception.getMostSpecificCause();
        String message = mostSpecificCause.getMessage();
        return message != null && message.toLowerCase().contains(IDEMPOTENCY_KEY_CONSTRAINT_MARKER);
    }

    @Override
    public void markAsRead(Long id, Long recipientUserId) {
        notificationRepository.markAsRead(id, recipientUserId, LocalDateTime.now(clock));
    }

    @Override
    public void delete(Long id, Long recipientUserId) {
        notificationRepository.delete(id, recipientUserId, LocalDateTime.now(clock));
    }

    @Override
    public int deleteRead(Long recipientUserId) {
        return notificationRepository.deleteAllReadByRecipientUserId(recipientUserId, LocalDateTime.now(clock));
    }
}
