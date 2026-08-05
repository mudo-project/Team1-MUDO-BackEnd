package com.academy.mudogroupware.users.presentation.api.request;

import java.util.Set;

import com.academy.mudogroupware.users.application.command.AssignRolePermissionsCommand;

import jakarta.validation.constraints.NotNull;

public record AssignRolePermissionsRequest(
        @NotNull Set<String> permissionCodes
) {

    public AssignRolePermissionsCommand toCommand(Long roleId, Long academyId) {
        return new AssignRolePermissionsCommand(roleId, academyId, permissionCodes);
    }
}
