package com.academy.mudogroupware.google.application.usecase;

import com.academy.mudogroupware.google.application.command.DisconnectGoogleAccountCommand;

public interface DisconnectGoogleAccountUseCase {

    void disconnect(DisconnectGoogleAccountCommand command);
}
