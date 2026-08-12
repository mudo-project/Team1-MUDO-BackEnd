package com.academy.mudogroupware.users.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Schema(description = "전화번호. 보내지 않으면 기존 값 유지", example = "010-1234-5678")
        @Size(max = 20) String phone,
        @Schema(description = "이메일. 보내지 않으면 기존 값 유지", example = "me@academy.kr")
        @Email @Size(max = 100) String email) {
}
