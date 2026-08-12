package com.academy.mudogroupware.global.domain.auth;

public record JwtClaims(Long userId, String username, Long roleId, AccountType accountType,
                         AdminScope adminScope, boolean mustChangePw) {}
