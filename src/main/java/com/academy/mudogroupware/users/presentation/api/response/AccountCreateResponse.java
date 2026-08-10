package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.application.result.CreateAccountResult;

public record AccountCreateResponse(Long userId, String username, String passwordSetupLink) {

    public static AccountCreateResponse from(CreateAccountResult result) {
        return new AccountCreateResponse(result.userId(), result.username(), result.passwordSetupLink());
    }
}
