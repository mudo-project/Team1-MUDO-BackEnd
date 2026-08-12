package com.academy.mudogroupware.notification.application.usecase;

import com.academy.mudogroupware.notification.application.command.CreateNotificationCommand;

public interface CreateNotificationUseCase {

    Long create(CreateNotificationCommand command);
}
