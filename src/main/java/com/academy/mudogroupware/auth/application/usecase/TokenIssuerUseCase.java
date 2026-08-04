package com.academy.mudogroupware.auth.application.usecase;

import com.academy.mudogroupware.auth.application.result.TokenPair;

public interface TokenIssuerUseCase {

  TokenPair issue(Long id, String username, Long roleId, Long academyId);

  String issueAccessToken(Long id, String username, Long roleId, Long academyId);
}
