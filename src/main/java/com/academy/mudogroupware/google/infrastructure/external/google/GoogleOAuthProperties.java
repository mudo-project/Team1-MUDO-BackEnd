package com.academy.mudogroupware.google.infrastructure.external.google;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.google.application.port.RequiredGoogleScopePort;

@Component
public record GoogleOAuthProperties(String clientId, String clientSecret, String redirectUri, String scope,
                                     String frontendRedirectUri) implements RequiredGoogleScopePort {

    // 템플릿 기능(드라이브·독스·시트)까지 포함한 scope다. 기존에 openid email만으로 연동된 계정은
    // GoogleAccountConnection.deriveStatus()가 이 값과 비교해 FAILED로 표시하고 재연결을 유도한다.
    // "email"이 아니라 정식 URL(https://www.googleapis.com/auth/userinfo.email)로 요청한다 — 구글은
    // 짧은 이름으로 요청해도 토큰 응답의 scope 필드에는 항상 정식 URL로 돌려주기 때문에, 우리가 요청할 때부터
    // 정식 URL을 쓰지 않으면 deriveStatus의 문자열 비교가 영원히 실패한다(email != 정식 URL).
    private static final String DEFAULT_SCOPE =
            "openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/drive.file "
            + "https://www.googleapis.com/auth/documents https://www.googleapis.com/auth/spreadsheets";

    public GoogleOAuthProperties(
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret,
            @Value("${GOOGLE_REDIRECT_URI:}") String redirectUri,
            @Value("${GOOGLE_OAUTH_SCOPE:" + DEFAULT_SCOPE + "}") String scope,
            @Value("${GOOGLE_OAUTH_FRONTEND_REDIRECT_URI:}") String frontendRedirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public Set<String> requiredScopes() {
        Set<String> scopes = Arrays.stream(scope.trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        if (scopes.isEmpty()) {
            throw new IllegalStateException("GOOGLE_OAUTH_SCOPE가 비어 있습니다.");
        }
        return scopes;
    }
}
