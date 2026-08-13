package com.academy.mudogroupware.platform.infrastructure.aws;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
public class PlatformAwsClientConfig {
  @Bean
  EcsClient platformEcsClient() {
    return EcsClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(currentRegion())
        .build();
  }

  private Region currentRegion() {
    return Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-northeast-2"));
  }
}
