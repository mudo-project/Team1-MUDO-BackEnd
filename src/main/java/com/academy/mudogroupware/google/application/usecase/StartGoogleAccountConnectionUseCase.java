package com.academy.mudogroupware.google.application.usecase;

import com.academy.mudogroupware.google.application.command.StartGoogleConnectionCommand;

public interface StartGoogleAccountConnectionUseCase {

    String start(StartGoogleConnectionCommand command);
}
