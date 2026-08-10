package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.AssignRolePermissionsCommand;

public interface AssignRolePermissionsUseCase {

    void assignPermissions(AssignRolePermissionsCommand command);
}
