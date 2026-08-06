package com.academy.mudogroupware.google.application.port;

public interface GoogleOAuthPort {

    /**
     * 구글 OAuth 동의 화면 URL을 만든다. forceAccountSelection이 true이면
     * (계정 교체) 구글이 항상 계정 선택 화면을 보여주도록 요청한다.
     */
    String buildAuthorizationUrl(String state, boolean forceAccountSelection);

    /**
     * 인가 코드를 액세스·리프레시 토큰으로 교환한다. 실패 시 {@link GoogleOAuthCallException}을 던진다.
     */
    GoogleTokenExchangeResult exchangeAuthorizationCode(String authorizationCode);

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 발급받아 유효성을 확인한다.
     * 실패 시 {@link GoogleOAuthCallException}을 던진다.
     */
    GoogleTokenExchangeResult refreshAccessToken(String refreshToken);

    /**
     * 액세스 토큰으로 연결된 구글 계정의 이메일을 조회한다. 실패 시 {@link GoogleOAuthCallException}을 던진다.
     */
    String fetchAccountEmail(String accessToken);

    /**
     * 토큰(리프레시 또는 액세스)을 구글 측에서 폐기한다. 실패해도 예외를 던지지 않고 무시한다(best-effort).
     */
    void revoke(String token);
}
