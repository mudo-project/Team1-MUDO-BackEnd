package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.security.jwt.*;
import com.academy.mudogroupware.global.presentation.security.*;
import jakarta.servlet.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
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
        new JwtAuthenticationFilter(provider, new JwtAuthenticationConverter());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(
        "Authorization", "Bearer " + provider.createAccessToken(7L, "teacher", "ADMIN"));
    filter.doFilter(request, new MockHttpServletResponse(), (q, s) -> {});
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("teacher");
  }
}
