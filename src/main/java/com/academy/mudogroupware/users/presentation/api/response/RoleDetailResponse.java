package com.academy.mudogroupware.users.presentation.api.response;

import java.util.Set;

import com.academy.mudogroupware.users.application.query.RoleView;

import io.swagger.v3.oas.annotations.media.Schema;

public record RoleDetailResponse(
        @Schema(description = "역할 ID", example = "3") Long roleId,
        @Schema(description = "역할 이름", example = "강사") String name,
        @Schema(description = "역할 설명", example = "수업 담당") String description,
        @Schema(description = "역할 뱃지 색상", example = "#FF5733") String color,
        @Schema(description = "이 역할을 쓰고 있는 재직 중(ACTIVE) 구성원 수", example = "4") long memberCount,
        @Schema(description = "이 역할에 담긴 권한 코드 목록") Set<String> permissionCodes) {

    public static RoleDetailResponse from(RoleView view) {
        return new RoleDetailResponse(
                view.role().getId(), view.role().getName(), view.role().getDescription(), view.role().getColor(),
                view.memberCount(), view.role().getPermissionCodes());
    }
}
