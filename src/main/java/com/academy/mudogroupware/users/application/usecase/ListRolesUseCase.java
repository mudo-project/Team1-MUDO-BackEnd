package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.users.domain.model.Role;

public interface ListRolesUseCase {

    List<Role> listRoles(Long academyId);
}
