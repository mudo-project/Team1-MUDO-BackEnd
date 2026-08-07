package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.domain.model.Role;

public record RoleListResponse(Long roleId, String name, String description) {

    public static RoleListResponse from(Role role) {
        return new RoleListResponse(role.getId(), role.getName(), role.getDescription());
    }
}
