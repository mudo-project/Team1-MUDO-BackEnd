package com.academy.mudogroupware.google.application.port;

public record GoogleOAuthStateClaims(Long academyId, Long userId, boolean forceAccountSelection) {
}
