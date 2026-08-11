package com.academy.mudogroupware.google.application.event;

// 트랜잭션 커밋 이후에만 구글 쪽 리프레시 토큰 폐기를 요청하기 위한 신호다.
public record OldGoogleRefreshTokenRevocationRequestedEvent(String oldRefreshToken) {
}
