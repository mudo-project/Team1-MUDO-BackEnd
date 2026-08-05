package com.academy.mudogroupware.global.infrastructure.observability;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.instance")
public class InstanceMetadataProperties {
  @NotBlank private String tenantId = "local";
  @NotBlank private String plan = "local";
  @NotBlank private String deploymentSha = "local";
}
