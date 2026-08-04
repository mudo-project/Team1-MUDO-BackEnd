package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.command.CompleteTaskCommand;

public interface CompleteTaskUseCase {

    void complete(CompleteTaskCommand command);
}
