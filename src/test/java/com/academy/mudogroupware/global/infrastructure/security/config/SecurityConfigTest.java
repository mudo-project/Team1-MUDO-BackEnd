package com.academy.mudogroupware.global.infrastructure.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationFilter;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAccessDeniedHandler;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAuthenticationEntryPoint;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {
  @Test
  void createsCorsConfigurationFromAllowedOrigins() {
    CorsProperties properties = new CorsProperties();
    properties.setAllowedOrigins(List.of("https://academy.example.com"));
    SecurityConfig securityConfig = securityConfig(properties);

    CorsConfigurationSource source = securityConfig.corsConfigurationSource();
    CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api"));

    assertThat(cors).isNotNull();
    assertThat(cors.getAllowedOriginPatterns()).containsExactly("https://academy.example.com");
    assertThat(cors.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
    assertThat(cors.getAllowedHeaders()).contains("Authorization");
    assertThat(cors.getExposedHeaders()).contains("Authorization");
    assertThat(cors.getAllowCredentials()).isTrue();
    assertThat(cors.getMaxAge()).isEqualTo(3600L);
  }

  @Test
  void exposesAuthenticationManagerFromSpringConfiguration() throws Exception {
    AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
    AuthenticationManager manager = mock(AuthenticationManager.class);
    when(configuration.getAuthenticationManager()).thenReturn(manager);

    assertThat(securityConfig(new CorsProperties()).authenticationManager(configuration))
        .isSameAs(manager);
  }

  private SecurityConfig securityConfig(CorsProperties properties) {
    return new SecurityConfig(
        mock(JwtAuthenticationFilter.class),
        mock(CustomAuthenticationEntryPoint.class),
        mock(CustomAccessDeniedHandler.class),
        properties);
  }
}
