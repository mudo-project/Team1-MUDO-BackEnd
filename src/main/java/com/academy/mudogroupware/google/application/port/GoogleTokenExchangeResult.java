package com.academy.mudogroupware.google.application.port;

public record GoogleTokenExchangeResult(
        String accessToken,
        String refreshToken,
        String scope,
        Long refreshTokenExpiresInSeconds) {

    public GoogleTokenExchangeResult(String accessToken, String refreshToken, String scope) {
        this(accessToken, refreshToken, scope, null);
    }
}
