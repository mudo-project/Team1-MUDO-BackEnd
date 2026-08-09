package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.CreateAccountCommand;
import com.academy.mudogroupware.users.application.result.CreateAccountResult;

public interface CreateAccountUseCase {

    CreateAccountResult createAccount(CreateAccountCommand command);
}
