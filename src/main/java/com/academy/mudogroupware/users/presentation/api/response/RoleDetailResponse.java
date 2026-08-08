package com.academy.mudogroupware.users.presentation.api.response;

import java.util.Set;

import com.academy.mudogroupware.users.domain.model.Role;

public record RoleDetailResponse(Long roleId, String name, String description, Set<String> permissionCodes) {

    public static RoleDetailResponse from(Role role) {
        return new RoleDetailResponse(role.getId(), role.getName(), role.getDescription(), role.getPermissionCodes());
    }
}
