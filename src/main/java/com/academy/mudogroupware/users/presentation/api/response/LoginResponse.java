package com.academy.mudogroupware.users.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.users.application.result.LoginResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "액세스 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "최초 비밀번호 설정이 아직 안 끝났는지 여부. true면 프론트가 비밀번호 설정 화면으로 안내해야 한다.",
                example = "false")
        boolean mustChangePw,
        @Schema(description = "이 계정이 최종적으로 갖는 권한 코드 전체 목록(합성 권한 포함). "
                + "서버가 @PreAuthorize로 실제 검사하는 것과 동일한 값이므로, 프론트는 이 목록에 코드가 포함돼있는지만 보고 "
                + "권한 없는 탭/메뉴를 숨기면 된다.",
                example = "[\"ACCOUNT:MANAGE\", \"ROLE:MANAGE\"]")
        List<String> permissions
) {

    public static LoginResponse from(LoginResult loginResult) {
        return new LoginResponse(
                loginResult.tokenPair().accessToken(), loginResult.mustChangePw(), loginResult.permissions());
    }
}
