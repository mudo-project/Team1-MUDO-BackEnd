package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.PasswordSetupCommand;

public interface PasswordSetupUseCase {

    void setup(PasswordSetupCommand command);
}
