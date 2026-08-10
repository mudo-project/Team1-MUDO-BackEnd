package com.academy.mudogroupware.users.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshResponse(
        @Schema(description = "새로 발급된 액세스 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {
}
