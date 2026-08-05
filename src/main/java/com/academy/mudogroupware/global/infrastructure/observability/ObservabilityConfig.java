package com.academy.mudogroupware.global.infrastructure.observability;

import com.academy.mudogroupware.global.infrastructure.filter.TraceIdFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(InstanceMetadataProperties.class)
public class ObservabilityConfig {

  @Bean
  TraceIdFilter traceIdFilter(InstanceMetadataProperties properties) {
    return new TraceIdFilter(properties);
  }

  @Bean
  MeterRegistryCustomizer<MeterRegistry> instanceCommonTags(
      InstanceMetadataProperties properties, Environment environment) {
    String[] activeProfiles = environment.getActiveProfiles();
    String environmentName = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);

    return registry ->
        registry
            .config()
            .commonTags(
                "service",
                environment.getProperty("spring.application.name", "mudo-groupware"),
                "environment",
                environmentName,
                "tenant",
                properties.getTenantId(),
                "plan",
                properties.getPlan(),
                "deployment",
                properties.getDeploymentSha());
  }
}
