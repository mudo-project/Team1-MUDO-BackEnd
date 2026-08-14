package com.academy.mudogroupware.planquota.infrastructure.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.planquota.application.port.TenantS3UsagePort;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

@ExtendWith(MockitoExtension.class)
class S3QuotaReconciliationSchedulerTest {

    @Mock
    private TenantS3UsagePort tenantS3UsagePort;
    @Mock
    private ResourceUsageQueryPort resourceUsageQueryPort;
    @Mock
    private ResourceUsageRecorder resourceUsageRecorder;

    private S3QuotaReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new S3QuotaReconciliationScheduler(tenantS3UsagePort, resourceUsageQueryPort, resourceUsageRecorder);
    }

    @Test
    void recordsPositiveAdjustmentWhenActualExceedsComputed() {
        when(tenantS3UsagePort.currentBytes()).thenReturn(1000L);
        when(resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE)).thenReturn(700L);

        scheduler.reconcile();

        verify(resourceUsageRecorder).recordS3Storage(new RecordS3StorageUsageCommand("reconciliation-adjustment", 300L));
    }

    @Test
    void doesNotAdjustWhenComputedExceedsActual() {
        when(tenantS3UsagePort.currentBytes()).thenReturn(700L);
        when(resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE)).thenReturn(1000L);

        scheduler.reconcile();

        verifyNoInteractions(resourceUsageRecorder);
    }

    @Test
    void doesNothingWhenInSync() {
        when(tenantS3UsagePort.currentBytes()).thenReturn(1000L);
        when(resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE)).thenReturn(1000L);

        scheduler.reconcile();

        verifyNoInteractions(resourceUsageRecorder);
    }
}
