package com.academy.mudogroupware.users.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.application.result.MemberListItem;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberListResponse(
        @Schema(description = "사용자 ID", example = "10") Long userId,
        @Schema(description = "이름", example = "최현우") String name,
        @Schema(description = "이메일", example = "hwchoi@academy.kr") String email,
        @Schema(description = "전화번호", example = "010-4567-8901") String phone,
        @Schema(description = "배정된 역할 ID. 없으면 null", example = "8") Long roleId,
        @Schema(description = "배정된 역할 이름. roleId가 없으면 null", example = "강사") String roleName,
        @Schema(description = "입사일", example = "2023-03-02T00:00:00") LocalDateTime joinedAt,
        @Schema(description = "계정 상태: ACTIVE/RESIGNED/INACTIVE", example = "ACTIVE") String status) {

    public static MemberListResponse from(MemberListItem item) {
        return new MemberListResponse(item.userId(), item.name(), item.email(), item.phone(), item.roleId(),
                item.roleName(), item.joinedAt(), item.status().name());
    }
}
