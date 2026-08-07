package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.domain.model.Role;

public interface GetRoleUseCase {

    Role getRole(Long roleId, Long academyId);
}
