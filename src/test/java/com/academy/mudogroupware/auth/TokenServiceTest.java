package com.academy.mudogroupware.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.academy.mudogroupware.auth.application.result.TokenPair;
import com.academy.mudogroupware.auth.application.service.TokenService;
import com.academy.mudogroupware.auth.infrastructure.persistence.*;
import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.JwtClaims;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import java.util.Optional;
import org.junit.jupiter.api.*;

class TokenServiceTest {
  @Test
  void replacesTokenForSameUser() {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    JwtTokenProvider provider = new JwtTokenProvider(p);
    RefreshTokenRepository r = mock(RefreshTokenRepository.class);
    RefreshTokenJpaEntity saved = new RefreshTokenJpaEntity(1L, "old");
    when(r.findByUserId(1L)).thenReturn(Optional.of(saved));
    TokenPair pair = new TokenService(provider, r).issue(1L, "user", 2L, AccountType.MEMBER, null);
    assertThat(saved.getRefreshToken()).isEqualTo(pair.refreshToken());
    verify(r, never()).save(any());
    assertThat(provider.parseAccessToken(pair.accessToken()))
        .isEqualTo(new JwtClaims(1L, "user", 2L, AccountType.MEMBER, null));
  }
}
