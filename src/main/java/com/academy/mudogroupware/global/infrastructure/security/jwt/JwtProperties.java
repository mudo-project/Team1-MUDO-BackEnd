package com.academy.mudogroupware.global.infrastructure.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
  private String secret = "local-development-only-change-this-secret-key";
  private long accessTokenExpiration = 3_600_000L;
  private long refreshTokenExpiration = 1_209_600_000L;
}
