package com.academy.mudogroupware.notification.domain.repository;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notification.domain.model.Notification;

public interface NotificationRepository {

    Notification save(Notification notification);

    PageResult<Notification> findAllByRecipientUserId(Long recipientUserId, int page, int size);

    long countUnreadByRecipientUserId(Long recipientUserId);

    void markAsRead(Long id, Long recipientUserId, LocalDateTime readAt);

    void delete(Long id, Long recipientUserId, LocalDateTime deletedAt);

    int deleteAllReadByRecipientUserId(Long recipientUserId, LocalDateTime deletedAt);
}
