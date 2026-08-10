package com.academy.mudogroupware.global.presentation.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.global.domain.auth.JwtClaims;
import com.academy.mudogroupware.global.domain.auth.PlatformAdminPermissionPort;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.domain.auth.RolePermissionLookupPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter {

  private final RolePermissionLookupPort rolePermissionLookupPort;
  private final PlatformAdminPermissionPort platformAdminPermissionPort;

  public Authentication toAuthentication(JwtClaims c) {
    boolean isPlatformAdmin = c.accountType() == AccountType.ADMIN && c.adminScope() == AdminScope.PLATFORM;
    boolean isAcademyOwner = c.accountType() == AccountType.ADMIN && c.adminScope() == AdminScope.ACADEMY;
    RolePermissionInfo info = isPlatformAdmin
        ? new RolePermissionInfo("SUPER_ADMIN", platformAdminPermissionPort.allPermissionCodes())
        : rolePermissionLookupPort.lookup(c.roleId());
    AuthUser p = new AuthUser(c.userId(), c.username(), c.academyId(), c.roleId(), info.roleName(),
        c.accountType(), c.adminScope());
    List<SimpleGrantedAuthority> authorities = new ArrayList<>(
        info.permissionCodes().stream().map(SimpleGrantedAuthority::new).toList());
    if (isPlatformAdmin) {
      authorities.add(new SimpleGrantedAuthority("PLATFORM:SUPER_ADMIN"));
    }
    if (isAcademyOwner) {
      authorities.add(new SimpleGrantedAuthority("ACADEMY:OWNER"));
    }
    return new UsernamePasswordAuthenticationToken(p, null, authorities);
  }
}
