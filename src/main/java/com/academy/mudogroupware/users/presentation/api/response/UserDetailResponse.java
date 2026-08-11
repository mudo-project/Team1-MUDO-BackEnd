package com.academy.mudogroupware.users.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.application.result.UserDetailResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserDetailResponse(
        @Schema(description = "사용자 ID", example = "10") Long userId,
        @Schema(description = "이름", example = "최현우") String name,
        @Schema(description = "이메일. 없으면 null", example = "hwchoi@academy.kr") String email,
        @Schema(description = "전화번호. 없으면 null", example = "010-4567-8901") String phone,
        @Schema(description = "배정된 역할 ID. 없으면 null", example = "8") Long roleId,
        @Schema(description = "배정된 역할 이름. roleId가 없으면 null", example = "강사") String roleName,
        @Schema(description = "입사일", example = "2023-03-02T00:00:00") LocalDateTime joinedAt,
        @Schema(description = "계정 상태: ACTIVE/RESIGNED/INACTIVE", example = "ACTIVE") String status) {

    public static UserDetailResponse from(UserDetailResult result) {
        return new UserDetailResponse(result.userId(), result.name(), result.email(), result.phone(),
                result.roleId(), result.roleName(), result.joinedAt(), result.status().name());
    }
}
