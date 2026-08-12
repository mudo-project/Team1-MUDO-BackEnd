package com.academy.mudogroupware.users.presentation.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.result.LoginResult;
import com.academy.mudogroupware.users.application.usecase.LoginUseCase;
import com.academy.mudogroupware.users.application.usecase.LogoutUseCase;
import com.academy.mudogroupware.users.presentation.api.common.UserResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.LoginRequest;
import com.academy.mudogroupware.users.presentation.api.response.LoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "계정·인증", description = "로그인/로그아웃 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Operation(
            summary = "로그인",
            description = "아이디/비밀번호로 로그인합니다. 액세스 토큰은 응답 바디로, 리프레시 토큰은 HttpOnly 쿠키로 내려갑니다.")
    @PostMapping("/login")
    public ResponseEntity<GlobalApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult loginResult = loginUseCase.login(request.toCommand());
        LoginResponse data = LoginResponse.from(loginResult);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.create(loginResult.tokenPair().refreshToken()).toString())
                .body(GlobalApiResponse.ok(UserResponseCode.LOGIN_SUCCEEDED, data));
    }

    @Operation(
            summary = "로그아웃",
            description = "서버에 저장된 리프레시 토큰을 삭제하고, 브라우저의 리프레시 토큰 쿠키를 즉시 만료시킵니다.")
    @PostMapping("/logout")
    public ResponseEntity<GlobalApiResponse<Void>> logout(@AuthenticationPrincipal AuthUser authUser) {
        logoutUseCase.logout(authUser.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .body(GlobalApiResponse.ok(UserResponseCode.LOGOUT_SUCCEEDED));
    }
}
