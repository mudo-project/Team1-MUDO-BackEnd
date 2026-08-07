package com.academy.mudogroupware.google.infrastructure.external.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record GoogleOAuthProperties(String clientId, String clientSecret, String redirectUri, String scope,
                                     String frontendRedirectUri) {

    // 이번 범위는 계정 연동(이메일 확인)까지다. drive.file/documents/spreadsheets는
    // 템플릿 기능을 실제로 구현할 때 점진적으로 요청한다(Google의 incremental authorization 권장 사항).
    private static final String DEFAULT_SCOPE = "openid email";

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
}
