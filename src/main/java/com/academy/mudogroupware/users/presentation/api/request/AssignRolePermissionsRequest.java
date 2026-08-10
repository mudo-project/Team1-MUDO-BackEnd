package com.academy.mudogroupware.users.presentation.api.request;

import java.util.Set;

import com.academy.mudogroupware.users.application.command.AssignRolePermissionsCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AssignRolePermissionsRequest(
        @Schema(
                description = "역할에 부여할 권한 코드 전체 목록(전체 교체 방식, 빈 배열이면 모든 권한 제거)",
                example = "[\"ROLE:MANAGE\", \"ACCOUNT:MANAGE\"]")
        @NotNull Set<String> permissionCodes
) {

    public AssignRolePermissionsCommand toCommand(Long roleId, Long academyId) {
        return new AssignRolePermissionsCommand(roleId, academyId, permissionCodes);
    }
}
