package com.academy.mudogroupware.notification.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.application.usecase.MarkNotificationAsReadUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService implements CreateNotificationUseCase, MarkNotificationAsReadUseCase {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Override
    public Long create(CreateNotificationCommand command) {
        Notification notification = Notification.create(
                command.recipientUserId(), command.type(), command.targetId(), command.message());
        return notificationRepository.save(notification).getId();
    }

    @Override
    public void markAsRead(Long id, Long recipientUserId) {
        notificationRepository.markAsRead(id, recipientUserId, LocalDateTime.now(clock));
    }
}
