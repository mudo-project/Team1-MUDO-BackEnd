package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.domain.model.Permission;

public record PermissionResponse(
        Long permissionId,
        String code,
        String resource,
        String action,
        String description
) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.id(), permission.code(), permission.resource(), permission.action(),
                permission.description());
    }
}
