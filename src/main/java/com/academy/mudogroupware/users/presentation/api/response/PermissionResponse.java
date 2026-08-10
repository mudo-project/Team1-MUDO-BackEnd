package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.domain.model.Permission;

import io.swagger.v3.oas.annotations.media.Schema;

public record PermissionResponse(
        @Schema(description = "권한 ID", example = "1") Long permissionId,
        @Schema(description = "권한 코드", example = "ROLE:MANAGE") String code,
        @Schema(description = "권한이 속한 리소스", example = "ROLE") String resource,
        @Schema(description = "리소스에 대한 행위", example = "MANAGE") String action,
        @Schema(description = "프론트에 그대로 표시 가능한 한글 설명", example = "역할 생성/수정/삭제 및 권한 조립") String description
) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.id(), permission.code(), permission.resource(), permission.action(),
                permission.description());
    }
}
