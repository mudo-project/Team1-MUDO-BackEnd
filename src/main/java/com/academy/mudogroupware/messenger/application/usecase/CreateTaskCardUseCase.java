package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.command.CreateTaskCardCommand;

public interface CreateTaskCardUseCase {

    Long createTaskCard(CreateTaskCardCommand command);
}
