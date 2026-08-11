package com.academy.mudogroupware.users.presentation.api.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateMemberProfileRequest(
        @Schema(description = "이름. 보내지 않으면 기존 값 유지", example = "최현우")
        @Size(max = 50) String name,
        @Schema(description = "전화번호. 보내지 않으면 기존 값 유지", example = "010-1234-5678")
        @Size(max = 20) String phone,
        @Schema(description = "이메일. 보내지 않으면 기존 값 유지", example = "hwchoi@academy.kr")
        @Size(max = 100) String email,
        @Schema(description = "입사일. 보내지 않으면 기존 값 유지", example = "2023-03-02T00:00:00")
        LocalDateTime joinedAt) {
}
