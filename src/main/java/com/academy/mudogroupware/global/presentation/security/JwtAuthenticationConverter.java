package com.academy.mudogroupware.global.presentation.security;

import com.academy.mudogroupware.global.domain.auth.JwtClaims;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter {
  public Authentication toAuthentication(JwtClaims c) {
    AuthUser p = new AuthUser(c.userId(), c.username(), c.role());
    return new UsernamePasswordAuthenticationToken(
        p, null, List.of(new SimpleGrantedAuthority(c.role())));
  }
}
