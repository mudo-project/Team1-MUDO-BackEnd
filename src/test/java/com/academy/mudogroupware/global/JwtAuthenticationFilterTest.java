package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import com.academy.mudogroupware.global.presentation.security.*;
import jakarta.servlet.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesBearerToken() throws Exception {
    JwtProperties p = new JwtProperties();
    p.setSecret("test-secret-key-that-is-at-least-32-bytes-long");
    JwtTokenProvider provider = new JwtTokenProvider(p);
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(
            provider,
            new JwtAuthenticationConverter(
                roleId -> new RolePermissionInfo("TEACHER", Set.of("WORKSPACE:READ")),
                Set::of));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(
        "Authorization", "Bearer " + provider.createAccessToken(7L, "teacher", 1L, AccountType.MEMBER, null));
    filter.doFilter(request, new MockHttpServletResponse(), (q, s) -> {});

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getName()).isEqualTo("teacher");
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("WORKSPACE:READ");
    AuthUser principal = (AuthUser) authentication.getPrincipal();
    assertThat(principal.roleName()).isEqualTo("TEACHER");
  }
}
