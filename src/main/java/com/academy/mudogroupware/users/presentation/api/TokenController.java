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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final RefreshUseCase refreshUseCase;

    @PostMapping("/reissue")
    public ResponseEntity<GlobalApiResponse<RefreshResponse>> reissue(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        String accessToken = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        return ResponseEntity.ok(
                GlobalApiResponse.ok(TokenResponseCode.ACCESS_TOKEN_REISSUED, new RefreshResponse(accessToken)));
    }
}
