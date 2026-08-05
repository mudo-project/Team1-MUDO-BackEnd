package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.command.UpdateMessageCommand;

public interface UpdateMessageUseCase {

    void update(UpdateMessageCommand command);
}
