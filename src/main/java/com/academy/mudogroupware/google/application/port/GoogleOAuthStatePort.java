package com.academy.mudogroupware.google.application.port;

public interface GoogleOAuthStatePort {

    /**
     * 콜백 위조·재사용을 막기 위해 userId를 서명해 state 문자열로 만든다.
     */
    String sign(GoogleOAuthStateClaims claims);

    /**
     * state 문자열의 서명과 만료 시각을 검증하고 원래 claims를 복원한다.
     * 서명이 유효하지 않거나 만료되었으면 {@link com.academy.mudogroupware.google.domain.exception.GoogleOAuthStateInvalidException}을 던진다.
     */
    GoogleOAuthStateClaims verify(String state);
}
