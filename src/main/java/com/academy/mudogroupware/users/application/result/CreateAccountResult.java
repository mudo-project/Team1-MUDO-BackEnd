package com.academy.mudogroupware.users.application.result;

public record CreateAccountResult(Long userId, String username, String passwordSetupLink) {
}
