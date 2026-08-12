package com.academy.mudogroupware.notification.application.usecase;

public interface MarkNotificationAsReadUseCase {

    void markAsRead(Long id, Long recipientUserId);
}
