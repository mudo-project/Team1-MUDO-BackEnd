package com.academy.mudogroupware.users.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.users.application.command.RefreshCommand;
import com.academy.mudogroupware.users.application.usecase.RefreshUseCase;
import com.academy.mudogroupware.users.presentation.api.common.TokenResponseCode;
import com.academy.mudogroupware.users.presentation.api.response.RefreshResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "계정·인증", description = "로그인/로그아웃 API")
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {

    private final RefreshUseCase refreshUseCase;

    @Operation(
            summary = "액세스 토큰 재발급",
            description = "리프레시 토큰 쿠키로 새 액세스 토큰을 발급합니다. 리프레시 토큰 자체는 로테이션하지 않습니다.")
    @PostMapping("/reissue")
    public ResponseEntity<GlobalApiResponse<RefreshResponse>> reissue(
            @CookieValue(name = RefreshTokenCookieFactory.REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        String accessToken = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        return ResponseEntity.ok(
                GlobalApiResponse.ok(TokenResponseCode.ACCESS_TOKEN_REISSUED, new RefreshResponse(accessToken)));
    }
}
