package com.academy.mudogroupware.users.presentation.api.response;

public record RoleCreateResponse(
        Long roleId
) {

    public static RoleCreateResponse from(Long roleId) {
        return new RoleCreateResponse(roleId);
    }
}
