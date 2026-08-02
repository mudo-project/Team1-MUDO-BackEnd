package com.academy.mudogroupware.global.infrastructure.security.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {
  private List<String> allowedOrigins =
      new ArrayList<>(List.of("http://localhost:3000", "http://localhost:8080"));
}
