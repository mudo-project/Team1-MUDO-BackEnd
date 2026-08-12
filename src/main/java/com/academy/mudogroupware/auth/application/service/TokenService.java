package com.academy.mudogroupware.auth.application.service;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.auth.application.usecase.RefreshTokenValidatorUseCase;
import com.academy.mudogroupware.auth.application.usecase.TokenIssuerUseCase;
import com.academy.mudogroupware.auth.application.usecase.TokenRevokerUseCase;
import com.academy.mudogroupware.auth.infrastructure.persistence.*;
import com.academy.mudogroupware.global.domain.auth.*;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService
    implements TokenIssuerUseCase, RefreshTokenValidatorUseCase, TokenRevokerUseCase {
  private final JwtTokenProvider provider;
  private final RefreshTokenRepository repository;

  @Override
  @Transactional
  public TokenPair issue(Long id, String username, Long roleId, AccountType accountType,
                          AdminScope adminScope) {
    String a = provider.createAccessToken(id, username, roleId, accountType, adminScope),
        r = provider.createRefreshToken(id, username);
    repository
        .findByUserId(id)
        .ifPresentOrElse(
            e -> e.replace(r), () -> repository.save(new RefreshTokenJpaEntity(id, r)));
    return new TokenPair(a, r);
  }

  @Override
  public String issueAccessToken(Long id, String username, Long roleId, AccountType accountType,
                                  AdminScope adminScope) {
    return provider.createAccessToken(id, username, roleId, accountType, adminScope);
  }

  @Override
  @Transactional(readOnly = true)
  public RefreshTokenClaims validateStored(String token) {
    RefreshTokenClaims c = provider.parseRefreshToken(token);
    RefreshTokenJpaEntity saved =
        repository
            .findByUserId(c.userId())
            .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
    if (!saved.getRefreshToken().equals(token))
      throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
    return c;
  }

  @Override
  @Transactional
  public void revoke(Long id) {
    repository.deleteByUserId(id);
  }
}
