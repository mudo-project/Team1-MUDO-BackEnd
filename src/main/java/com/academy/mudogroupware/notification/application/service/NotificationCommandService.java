package com.academy.mudogroupware.notification.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;
import com.academy.mudogroupware.notification.application.usecase.CreateNotificationUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService implements CreateNotificationUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public Long create(CreateNotificationCommand command) {
        Notification notification = Notification.create(
                command.recipientUserId(), command.type(), command.targetId(), command.message());
        return notificationRepository.save(notification).getId();
    }
}
