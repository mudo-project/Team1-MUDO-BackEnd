package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.DeleteRoleCommand;

public interface DeleteRoleUseCase {

    void deleteRole(DeleteRoleCommand command);
}
