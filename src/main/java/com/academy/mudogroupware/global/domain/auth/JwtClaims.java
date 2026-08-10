package com.academy.mudogroupware.global.domain.auth;

public record JwtClaims(Long userId, String username, Long roleId, Long academyId, AccountType accountType,
                         AdminScope adminScope) {}
