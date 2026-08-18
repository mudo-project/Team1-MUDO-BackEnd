package com.academy.mudogroupware.platform.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// PlatformDashboardProperties는 tenant-directory-json처럼 dashboard host가 아닌 Task도
// 읽어야 하는 값을 담고 있어 platform.dashboard.enabled 조건 없이 항상 등록한다. 관리자
// 대시보드 전용 조회 Bean(PlatformDashboardQueryService 등)은 각자 그 조건을 유지한다.
@Configuration
@EnableConfigurationProperties(PlatformDashboardProperties.class)
public class PlatformDashboardConfiguration {}
