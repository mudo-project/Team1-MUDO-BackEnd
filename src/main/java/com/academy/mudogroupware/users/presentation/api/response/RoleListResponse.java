package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.application.query.RoleView;

public record RoleListResponse(Long roleId, String name, String description, String color, long memberCount) {

    public static RoleListResponse from(RoleView view) {
        return new RoleListResponse(
                view.role().getId(), view.role().getName(), view.role().getDescription(), view.role().getColor(),
                view.memberCount());
    }
}
