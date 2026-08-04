package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.RegisterPushSubscriptionCommand;

public interface RegisterPushSubscriptionUseCase {

    Long register(RegisterPushSubscriptionCommand command);
}
