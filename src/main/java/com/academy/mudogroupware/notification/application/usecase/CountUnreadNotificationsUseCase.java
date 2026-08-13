package com.academy.mudogroupware.notification.application.usecase;

public interface CountUnreadNotificationsUseCase {

    long countUnread(Long recipientUserId);
}
