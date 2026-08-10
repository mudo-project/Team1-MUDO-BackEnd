package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.*;

import com.academy.mudogroupware.global.domain.auth.*;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.*;

class JwtTokenProviderTest {
  private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    JwtProperties p = new JwtProperties();
    p.setSecret(SECRET);
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
  void parseAccessTokenDefaultsAccountTypeAndAdminScopeForLegacyTokenWithoutThoseClaims() {
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    String legacyToken = Jwts.builder()
        .subject("5")
        .claim("username", "legacy-user")
        .claim("roleId", 7L)
        .claim("academyId", 3L)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + 3_600_000))
        .signWith(key, Jwts.SIG.HS256)
        .compact();

    assertThat(provider.parseAccessToken(legacyToken))
        .isEqualTo(new JwtClaims(5L, "legacy-user", 7L, 3L, AccountType.MEMBER, null));
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
