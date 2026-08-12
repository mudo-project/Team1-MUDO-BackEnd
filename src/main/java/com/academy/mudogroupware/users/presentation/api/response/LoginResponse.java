package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.application.result.LoginResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "액세스 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "최초 비밀번호 설정이 아직 안 끝났는지 여부. true면 프론트가 비밀번호 설정 화면으로 안내해야 한다.",
                example = "false")
        boolean mustChangePw
) {

    public static LoginResponse from(LoginResult loginResult) {
        return new LoginResponse(loginResult.tokenPair().accessToken(), loginResult.mustChangePw());
    }
}
