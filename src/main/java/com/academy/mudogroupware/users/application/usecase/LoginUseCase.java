package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.LoginCommand;
import com.academy.mudogroupware.users.application.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
