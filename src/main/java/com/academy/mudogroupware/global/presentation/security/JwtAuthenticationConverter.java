package com.academy.mudogroupware.global.presentation.security;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.domain.auth.JwtClaims;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.domain.auth.RolePermissionLookupPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter {

  private final RolePermissionLookupPort rolePermissionLookupPort;

  public Authentication toAuthentication(JwtClaims c) {
    RolePermissionInfo info = rolePermissionLookupPort.lookup(c.roleId());
    AuthUser p = new AuthUser(c.userId(), c.username(), c.academyId(), c.roleId(), info.roleName());
    List<SimpleGrantedAuthority> authorities =
        info.permissionCodes().stream().map(SimpleGrantedAuthority::new).toList();
    return new UsernamePasswordAuthenticationToken(p, null, authorities);
  }
}
