package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.query.RoleView;

public interface GetRoleUseCase {

    RoleView getRole(Long roleId);
}
