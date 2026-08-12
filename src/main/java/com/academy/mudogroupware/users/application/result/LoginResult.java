package com.academy.mudogroupware.users.application.result;

import com.academy.mudogroupware.auth.application.result.TokenPair;

public record LoginResult(TokenPair tokenPair, boolean mustChangePw) {
}
