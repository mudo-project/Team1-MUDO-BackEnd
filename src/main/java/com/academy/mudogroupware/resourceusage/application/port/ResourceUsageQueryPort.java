package com.academy.mudogroupware.resourceusage.application.port;

import java.time.LocalDateTime;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

/** file/payroll 등 다른 도메인이 resourceusage 내부 리포지토리를 직접 참조하지 않고
 *  현재 사용량 합계를 조회하기 위한 포트다. */
public interface ResourceUsageQueryPort {
    long sumByType(ResourceUsageType type);

    long sumByTypeAndPeriod(ResourceUsageType type, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
