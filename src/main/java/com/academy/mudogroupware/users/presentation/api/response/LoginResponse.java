package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.auth.application.result.TokenPair;

public record LoginResponse(
        String accessToken
) {

    public static LoginResponse from(TokenPair tokenPair) {
        return new LoginResponse(tokenPair.accessToken());
    }
}
