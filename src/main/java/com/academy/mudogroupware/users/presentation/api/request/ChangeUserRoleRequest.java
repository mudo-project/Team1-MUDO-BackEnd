package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.ChangeUserRoleCommand;

import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(@NotNull Long roleId) {

    public ChangeUserRoleCommand toCommand(Long userId, Long academyId) {
        return new ChangeUserRoleCommand(userId, academyId, roleId);
    }
}
