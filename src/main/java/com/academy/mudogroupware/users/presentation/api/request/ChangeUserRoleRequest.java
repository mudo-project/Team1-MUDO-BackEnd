package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.ChangeUserRoleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(
        @Schema(description = "배정할 역할 ID (같은 학원 소속 역할만 가능)", example = "5")
        @NotNull Long roleId) {

    public ChangeUserRoleCommand toCommand(Long userId) {
        return new ChangeUserRoleCommand(userId, roleId);
    }
}
