package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.*;

import com.academy.mudogroupware.global.domain.auth.*;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import org.junit.jupiter.api.*;

class JwtTokenProviderTest {
  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    provider = new JwtTokenProvider(p);
  }

  @Test
  void accessTokenRoundTrip() {
    String t = provider.createAccessToken(1L, "academy-user", 10L, 20L, AccountType.MEMBER, null);
    assertThat(provider.parseAccessToken(t))
        .isEqualTo(new JwtClaims(1L, "academy-user", 10L, 20L, AccountType.MEMBER, null));
  }

  @Test
  void accessTokenRoundTripForPlatformAdmin() {
    String t = provider.createAccessToken(9L, "super-admin", null, 99L, AccountType.ADMIN, AdminScope.PLATFORM);
    assertThat(provider.parseAccessToken(t))
        .isEqualTo(new JwtClaims(9L, "super-admin", null, 99L, AccountType.ADMIN, AdminScope.PLATFORM));
  }

  @Test
  void refreshTokenRoundTrip() {
    String t = provider.createRefreshToken(2L, "teacher");
    assertThat(provider.parseRefreshToken(t)).isEqualTo(new RefreshTokenClaims(2L, "teacher"));
  }

  @Test
  void rejectsInvalidToken() {
    assertThatThrownBy(() -> provider.parseAccessToken("invalid"))
        .isInstanceOf(AuthException.class);
  }
}
