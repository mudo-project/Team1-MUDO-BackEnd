package com.academy.mudogroupware.planquota.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.planquota.application.port.TenantS3UsagePort;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * S3_STORAGE 합계는 판단의 주 기준(RegisterFileService에서 실시간 체크)이고,
 * 이 스케줄러는 드리프트를 보정하는 안전장치일 뿐이라 자주 돌 필요가 없다.
 * ResourceUsageEvent가 amount<0을 허용하지 않으므로(누락분만 보정), 실측값이
 * 계산값보다 작은 경우(과다 카운트)는 자동 보정하지 않고 로그만 남긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3QuotaReconciliationScheduler {

    private final TenantS3UsagePort tenantS3UsagePort;
    private final ResourceUsageQueryPort resourceUsageQueryPort;
    private final ResourceUsageRecorder resourceUsageRecorder;

    @Scheduled(cron = "${app.planquota.s3-reconciliation-cron:0 0 3 * * *}")
    public void reconcile() {
        long actual = tenantS3UsagePort.currentBytes();
        long computed = resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE);
        long delta = actual - computed;
        if (delta > 0) {
            resourceUsageRecorder.recordS3Storage(
                    new RecordS3StorageUsageCommand("reconciliation-adjustment", delta));
            log.info("event=s3_quota_reconcile_보정 delta={} actual={} computed={}", delta, actual, computed);
        } else if (delta < 0) {
            log.warn("event=s3_quota_reconcile_불일치_감소 actual={} computed={}", actual, computed);
        }
    }
}
