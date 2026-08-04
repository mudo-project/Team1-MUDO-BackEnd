package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.users.application.command.LoginCommand;

public interface LoginUseCase {

    TokenPair login(LoginCommand command);
}
