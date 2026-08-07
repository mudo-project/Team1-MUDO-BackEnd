package com.academy.mudogroupware.google.application.usecase;

import com.academy.mudogroupware.google.application.command.CompleteGoogleConnectionCommand;

public interface CompleteGoogleAccountConnectionUseCase {

    void complete(CompleteGoogleConnectionCommand command);
}
