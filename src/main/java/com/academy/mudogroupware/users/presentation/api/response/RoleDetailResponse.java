package com.academy.mudogroupware.users.presentation.api.response;

import java.util.Set;

import com.academy.mudogroupware.users.application.query.RoleView;

public record RoleDetailResponse(
        Long roleId, String name, String description, String color, long memberCount, Set<String> permissionCodes) {

    public static RoleDetailResponse from(RoleView view) {
        return new RoleDetailResponse(
                view.role().getId(), view.role().getName(), view.role().getDescription(), view.role().getColor(),
                view.memberCount(), view.role().getPermissionCodes());
    }
}
