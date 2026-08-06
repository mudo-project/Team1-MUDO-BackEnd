package com.academy.mudogroupware.users.application.usecase;

import java.util.List;

import com.academy.mudogroupware.users.domain.model.Permission;

public interface PermissionQueryUseCase {

    List<Permission> getPermissions();
}
