package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.ChangeUserRoleCommand;

public interface ChangeUserRoleUseCase {

    void changeRole(ChangeUserRoleCommand command);
}
