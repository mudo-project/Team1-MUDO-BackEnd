package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RevenueReportAiEngineProperties.class)
class RevenueReportAiConfig {
}
