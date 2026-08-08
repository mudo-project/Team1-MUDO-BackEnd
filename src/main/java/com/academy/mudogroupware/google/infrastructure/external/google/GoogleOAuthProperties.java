package com.academy.mudogroupware.google.infrastructure.external.google;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record GoogleOAuthProperties(String clientId, String clientSecret, String redirectUri, String scope,
                                     String frontendRedirectUri) {

    // 템플릿 기능(드라이브·독스·시트)까지 포함한 scope다. 기존에 openid email만으로 연동된 계정은
    // GoogleAccountConnection.deriveStatus()가 이 값과 비교해 FAILED로 표시하고 재연결을 유도한다.
    private static final String DEFAULT_SCOPE =
            "openid email https://www.googleapis.com/auth/drive.file "
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

    public Set<String> scopeSet() {
        return Set.of(scope.trim().split("\\s+"));
    }
}
