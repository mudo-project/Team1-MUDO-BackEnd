package com.academy.mudogroupware.platform.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PlatformDashboardProperties.class)
public class PlatformDashboardConfiguration {}
