package com.academy.mudogroupware.google.application.usecase;

import com.academy.mudogroupware.google.application.command.CheckGoogleConnectionCommand;

public interface CheckGoogleAccountConnectionUseCase {

    void check(CheckGoogleConnectionCommand command);
}
