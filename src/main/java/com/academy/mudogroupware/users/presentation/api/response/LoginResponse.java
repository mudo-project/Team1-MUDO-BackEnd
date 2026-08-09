package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.auth.application.result.TokenPair;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "액세스 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {

    public static LoginResponse from(TokenPair tokenPair) {
        return new LoginResponse(tokenPair.accessToken());
    }
}
