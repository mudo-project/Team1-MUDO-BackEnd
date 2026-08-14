package com.academy.mudogroupware.planquota.application.port;

/** platform.StorageUsagePort와 달리 platform.dashboard.enabled 플래그에 의존하지
 *  않는, 이 테넌트 자신의 S3 사용량만 항상 조회 가능한 포트다. */
public interface TenantS3UsagePort {
    long currentBytes();
}
