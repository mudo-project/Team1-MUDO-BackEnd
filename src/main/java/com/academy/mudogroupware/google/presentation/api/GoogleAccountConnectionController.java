package com.academy.mudogroupware.google.presentation.api;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.google.application.command.CompleteGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.command.StartGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.usecase.CheckGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.application.usecase.CompleteGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.application.usecase.DisconnectGoogleAccountUseCase;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccountConnectionStatusUseCase;
import com.academy.mudogroupware.google.application.usecase.StartGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.presentation.api.common.GoogleResponseCode;
import com.academy.mudogroupware.google.presentation.api.response.GoogleAccountConnectionResponse;
import com.academy.mudogroupware.google.presentation.api.response.GoogleAccountConnectionStatusResponse;
import com.academy.mudogroupware.google.presentation.api.response.GoogleAuthorizationUrlResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "구글 연동", description = "학원 명의 구글 계정 연동(OAuth) 관리 API")
@RestController
@RequestMapping("/api/google/connections")
@RequiredArgsConstructor
public class GoogleAccountConnectionController {

    private final StartGoogleAccountConnectionUseCase startGoogleAccountConnectionUseCase;
    private final CompleteGoogleAccountConnectionUseCase completeGoogleAccountConnectionUseCase;
    private final GetGoogleAccountConnectionUseCase getGoogleAccountConnectionUseCase;
    private final GetGoogleAccountConnectionStatusUseCase getGoogleAccountConnectionStatusUseCase;
    private final CheckGoogleAccountConnectionUseCase checkGoogleAccountConnectionUseCase;
    private final DisconnectGoogleAccountUseCase disconnectGoogleAccountUseCase;

    @Value("${GOOGLE_OAUTH_FRONTEND_REDIRECT_URI:/}")
    private String frontendRedirectUri;

    @Operation(summary = "구글 계정 연동 시작", description = "구글 OAuth 동의 화면으로 이동할 인증 URL을 발급합니다. "
            + "switchAccount=true이면 계정 교체를 위해 구글 계정 선택 화면을 강제로 띄웁니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증 URL 발급 성공"),
        @ApiResponse(responseCode = "403", description = "원장(academy 관리자) 계정이 아닌 경우")
    })
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @PostMapping("/authorize-url")
    public ResponseEntity<GlobalApiResponse<GoogleAuthorizationUrlResponse>> startConnection(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "false") boolean switchAccount) {
        String authorizationUrl = startGoogleAccountConnectionUseCase.start(
                new StartGoogleConnectionCommand(authUser.userId(), switchAccount));
        return ResponseEntity.ok(GlobalApiResponse.ok(
                GoogleResponseCode.AUTHORIZATION_URL_ISSUED, GoogleAuthorizationUrlResponse.from(authorizationUrl)));
    }

    @Operation(summary = "구글 OAuth 콜백", description = "구글 동의 화면 완료 후 브라우저가 돌아오는 리다이렉트 대상입니다. "
            + "인가 코드를 토큰으로 교환해 연동을 저장하고 프론트엔드로 리다이렉트합니다.")
    @GetMapping("/callback")
    // 실패 원인(Google 오류/파라미터 누락/연동 처리 예외)을 구분해 로그에 남기되, code·state(토큰·인가코드가
    // 실린 값)는 절대 로그에 남기지 않는다.
    public ResponseEntity<Void> completeConnection(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        String redirectUri;
        if (error != null) {
            log.warn("event=google_oauth_callback_google_error error={}", sanitizeForLog(error));
            redirectUri = frontendRedirectUri + "?googleConnection=failed";
        } else if (code == null || code.isBlank() || state == null || state.isBlank()) {
            log.warn("event=google_oauth_callback_missing_params codePresent={} statePresent={}",
                    code != null && !code.isBlank(), state != null && !state.isBlank());
            redirectUri = frontendRedirectUri + "?googleConnection=failed";
        } else {
            try {
                completeGoogleAccountConnectionUseCase.complete(new CompleteGoogleConnectionCommand(code, state));
                redirectUri = frontendRedirectUri + "?googleConnection=success";
            } catch (RuntimeException e) {
                log.warn("event=google_oauth_callback_failed exceptionType={}", e.getClass().getSimpleName());
                redirectUri = frontendRedirectUri + "?googleConnection=failed";
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUri)).build();
    }

    // 로그 위조(CR/LF 삽입)와 과도한 길이의 로그를 막기 위해 요청 제어값(error)을 제어 문자 제거 +
    // 길이 제한 후에만 로그에 남긴다. error는 permitAll 엔드포인트라 공격자가 값을 완전히 통제할 수 있다.
    private static String sanitizeForLog(String value) {
        String sanitized = value.replaceAll("[\\r\\n\\t]", "_");
        return sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
    }

    @Operation(summary = "구글 연동 상태 조회", description = "현재 학원의 구글 계정 연동 상태를 조회합니다. 연동된 계정이 없으면 data가 null입니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "원장(academy 관리자) 계정이 아닌 경우")
    })
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<GoogleAccountConnectionResponse>> getConnection() {
        GoogleAccountConnectionResponse response = getGoogleAccountConnectionUseCase
                .getConnection()
                .map(GoogleAccountConnectionResponse::from)
                .orElse(null);
        return ResponseEntity.ok(GlobalApiResponse.ok(GoogleResponseCode.CONNECTION_RETRIEVED, response));
    }

    @Operation(summary = "구글 연동 상태 요약 조회", description = "화면 요약 카드에 표시할 현재 구글 연동 상태만 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "원장(academy 관리자) 계정이 아닌 경우")
    })
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @GetMapping("/status")
    public ResponseEntity<GlobalApiResponse<GoogleAccountConnectionStatusResponse>> getConnectionStatus() {
        return ResponseEntity.ok(GlobalApiResponse.ok(
                GoogleResponseCode.CONNECTION_STATUS_RETRIEVED,
                new GoogleAccountConnectionStatusResponse(getGoogleAccountConnectionStatusUseCase.getStatus())));
    }

    @Operation(summary = "구글 연동 상태 확인", description = "저장된 리프레시 토큰으로 구글에 실제 유효성을 확인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "확인 성공"),
        @ApiResponse(responseCode = "403", description = "원장(academy 관리자) 계정이 아닌 경우"),
        @ApiResponse(responseCode = "404", description = "연동된 구글 계정이 없는 경우")
    })
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @PostMapping("/check")
    public ResponseEntity<Void> checkConnection() {
        checkGoogleAccountConnectionUseCase.check();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "구글 연동 해제", description = "구글 계정 연동을 해제하고 저장된 토큰을 폐기합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "해제 성공"),
        @ApiResponse(responseCode = "403", description = "원장(academy 관리자) 계정이 아닌 경우"),
        @ApiResponse(responseCode = "404", description = "연동된 구글 계정이 없는 경우")
    })
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @DeleteMapping
    public ResponseEntity<Void> disconnect() {
        disconnectGoogleAccountUseCase.disconnect();
        return ResponseEntity.noContent().build();
    }
}
