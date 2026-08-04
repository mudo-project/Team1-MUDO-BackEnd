package com.academy.mudogroupware.users.presentation.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.usecase.LoginUseCase;
import com.academy.mudogroupware.users.application.usecase.LogoutUseCase;
import com.academy.mudogroupware.users.presentation.api.common.UserResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.LoginRequest;
import com.academy.mudogroupware.users.presentation.api.response.LoginResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<GlobalApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = loginUseCase.login(request.toCommand());
        LoginResponse data = LoginResponse.from(tokenPair);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(tokenPair.refreshToken()).toString())
                .body(GlobalApiResponse.ok(UserResponseCode.LOGIN_SUCCEEDED, data));
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalApiResponse<Void>> logout(@AuthenticationPrincipal AuthUser authUser) {
        logoutUseCase.logout(authUser.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .body(GlobalApiResponse.ok(UserResponseCode.LOGOUT_SUCCEEDED));
    }
}
