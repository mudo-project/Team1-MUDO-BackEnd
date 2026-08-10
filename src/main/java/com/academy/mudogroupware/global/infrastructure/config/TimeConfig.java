package com.academy.mudogroupware.global.infrastructure.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class TimeConfig {

  public static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  @Bean
  public Clock clock() {
    return Clock.system(KOREA_ZONE);
  }

  @Bean
  public DateTimeProvider auditingDateTimeProvider(Clock clock) {
    return () -> Optional.of(LocalDateTime.now(clock));
  }
}
