package com.academy.mudogroupware.users.presentation.api;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtProperties;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.users.application.usecase.LoginUseCase;
import com.academy.mudogroupware.users.presentation.api.common.UserResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.LoginRequest;
import com.academy.mudogroupware.users.presentation.api.response.LoginResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final LoginUseCase loginUseCase;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<GlobalApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = loginUseCase.login(request.toCommand());
        LoginResponse data = LoginResponse.from(tokenPair);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokenPair.refreshToken()).toString())
                .body(GlobalApiResponse.ok(UserResponseCode.LOGIN_SUCCEEDED, data));
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();
    }
}
