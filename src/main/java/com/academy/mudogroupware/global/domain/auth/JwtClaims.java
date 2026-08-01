package com.academy.mudogroupware.global.domain.auth;
public record JwtClaims(Long userId, String username, String role) {}
