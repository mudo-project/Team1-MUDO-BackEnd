package com.academy.mudogroupware.notification.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notification.application.usecase.CountUnreadNotificationsUseCase;
import com.academy.mudogroupware.notification.application.usecase.ListNotificationsUseCase;
import com.academy.mudogroupware.notification.domain.model.Notification;
import com.academy.mudogroupware.notification.domain.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService implements ListNotificationsUseCase, CountUnreadNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    public PageResult<Notification> getNotifications(Long recipientUserId, int page, int size) {
        return notificationRepository.findAllByRecipientUserId(recipientUserId, page, size);
    }

    @Override
    public long countUnread(Long recipientUserId) {
        return notificationRepository.countUnreadByRecipientUserId(recipientUserId);
    }
}
