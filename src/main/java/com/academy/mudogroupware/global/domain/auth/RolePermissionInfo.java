package com.academy.mudogroupware.global.domain.auth;

import java.util.Set;

public record RolePermissionInfo(String roleName, Set<String> permissionCodes) {

    public static RolePermissionInfo empty() {
        return new RolePermissionInfo(null, Set.of());
    }
}
