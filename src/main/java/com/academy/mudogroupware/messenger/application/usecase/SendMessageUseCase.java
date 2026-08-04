package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.command.SendMessageCommand;

public interface SendMessageUseCase {

    Long sendMessage(SendMessageCommand command);
}
