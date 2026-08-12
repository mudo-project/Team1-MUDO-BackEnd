package com.academy.mudogroupware.auth.application.usecase;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;

public interface TokenIssuerUseCase {

  TokenPair issue(Long id, String username, Long roleId, AccountType accountType,
                   AdminScope adminScope, boolean mustChangePw);

  String issueAccessToken(Long id, String username, Long roleId, AccountType accountType,
                           AdminScope adminScope, boolean mustChangePw);
}
