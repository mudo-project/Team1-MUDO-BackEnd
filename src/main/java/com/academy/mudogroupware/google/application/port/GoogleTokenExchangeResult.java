package com.academy.mudogroupware.google.application.port;

public record GoogleTokenExchangeResult(String accessToken, String refreshToken, String scope) {
}
