package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.users.application.query.RoleView;

public interface ListRolesUseCase {

    List<RoleView> listRoles(Long academyId);
}
