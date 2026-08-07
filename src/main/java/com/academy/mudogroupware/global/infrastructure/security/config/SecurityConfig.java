package com.academy.mudogroupware.global.infrastructure.security.config;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtProperties;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationFilter;
import com.academy.mudogroupware.global.presentation.security.handler.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwt;
  private final CustomAuthenticationEntryPoint entry;
  private final CustomAccessDeniedHandler denied;
  private final CorsProperties corsProperties;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity h) throws Exception {
    return h.cors(c -> c.configurationSource(corsConfigurationSource()))
        .csrf(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/error", "/actuator/health", "/actuator/prometheus", "/ws/**")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/token/reissue")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/google/connections/callback")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/academy-applications")
                    .hasAuthority("PLATFORM:SUPER_ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}
