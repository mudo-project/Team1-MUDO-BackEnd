package com.academy.mudogroupware.messenger.application.usecase;

import com.academy.mudogroupware.messenger.application.command.DeleteTaskCardCommand;

public interface DeleteTaskCardUseCase {

    void delete(DeleteTaskCardCommand command);
}
