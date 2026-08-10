package com.academy.mudogroupware.global.infrastructure.security.jwt;

import com.academy.mudogroupware.global.domain.auth.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final JwtProperties p;
  private final SecretKey key;

  public JwtTokenProvider(JwtProperties p) {
    this.p = p;
    this.key = Keys.hmacShaKeyFor(p.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(Long id, String username, Long roleId, Long academyId, AccountType accountType,
                                   AdminScope adminScope) {
    Date n = new Date();
    return Jwts.builder()
        .subject(String.valueOf(id))
        .claim("username", username)
        .claim("roleId", roleId)
        .claim("academyId", academyId)
        .claim("accountType", accountType.name())
        .claim("adminScope", adminScope == null ? null : adminScope.name())
        .issuedAt(n)
        .expiration(new Date(n.getTime() + p.getAccessTokenExpiration()))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public String createRefreshToken(Long id, String username) {
    Date n = new Date();
    return Jwts.builder()
        .subject(String.valueOf(id))
        .claim("username", username)
        .issuedAt(n)
        .expiration(new Date(n.getTime() + p.getRefreshTokenExpiration()))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      claims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public JwtClaims parseAccessToken(String token) {
    Claims c = claims(token);
    String u = c.get("username", String.class);
    if (u == null || u.isBlank()) throw new AuthException(AuthErrorCode.USERNAME_CLAIM_MISSING);
    Long roleId = c.get("roleId", Long.class);
    Long academyId = c.get("academyId", Long.class);
    String accountTypeRaw = c.get("accountType", String.class);
    AccountType accountType = accountTypeRaw == null ? AccountType.MEMBER : AccountType.valueOf(accountTypeRaw);
    String adminScopeRaw = c.get("adminScope", String.class);
    AdminScope adminScope = adminScopeRaw == null ? null : AdminScope.valueOf(adminScopeRaw);
    return new JwtClaims(id(c), u, roleId, academyId, accountType, adminScope);
  }

  public RefreshTokenClaims parseRefreshToken(String token) {
    Claims c = claims(token);
    String u = c.get("username", String.class);
    if (u == null || u.isBlank()) throw new AuthException(AuthErrorCode.USERNAME_CLAIM_MISSING);
    return new RefreshTokenClaims(id(c), u);
  }

  private Claims claims(String token) {
    try {
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      throw new AuthException(AuthErrorCode.EXPIRED_TOKEN, e);
    } catch (JwtException | IllegalArgumentException e) {
      throw new AuthException(AuthErrorCode.INVALID_TOKEN, e);
    }
  }

  private Long id(Claims c) {
    try {
      String s = c.getSubject();
      if (s == null || s.isBlank()) throw new NumberFormatException();
      return Long.valueOf(s);
    } catch (NumberFormatException e) {
      throw new AuthException(AuthErrorCode.INVALID_TOKEN_SUBJECT, e);
    }
  }
}
