package com.academy.mudogroupware.users.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RoleCreateResponse(
        @Schema(description = "생성된 역할 ID", example = "3")
        Long roleId
) {

    public static RoleCreateResponse from(Long roleId) {
        return new RoleCreateResponse(roleId);
    }
}
