package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.CreateRoleCommand;

public interface CreateRoleUseCase {

    Long createRole(CreateRoleCommand command);
}
