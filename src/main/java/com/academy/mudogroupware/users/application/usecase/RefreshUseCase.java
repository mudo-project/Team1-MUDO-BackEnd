package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.RefreshCommand;

public interface RefreshUseCase {

    String refresh(RefreshCommand command);
}
