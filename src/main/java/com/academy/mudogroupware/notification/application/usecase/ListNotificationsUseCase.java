package com.academy.mudogroupware.notification.application.usecase;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notification.domain.model.Notification;

public interface ListNotificationsUseCase {

    PageResult<Notification> getNotifications(Long recipientUserId, int page, int size);
}
