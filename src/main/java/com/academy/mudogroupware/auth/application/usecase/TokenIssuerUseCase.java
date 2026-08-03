package com.academy.mudogroupware.auth.application.usecase;

import com.academy.mudogroupware.auth.application.result.TokenPair;

public interface TokenIssuerUseCase {

  TokenPair issue(Long id, String username, String role);

  String issueAccessToken(Long id, String username, String role);
}
