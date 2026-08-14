package com.academy.mudogroupware.planquota.infrastructure.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.academy.mudogroupware.planquota.application.service.DatabaseQuotaAspectProbeService;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.platform.application.port.CurrentTenantDatabaseUsagePort;

@SpringBootTest
class DatabaseQuotaAspectTest {

    @MockitoBean
    private CurrentTenantDatabaseUsagePort databaseUsagePort;

    @Autowired
    private DatabaseQuotaAspectProbeService probeService;

    @Test
    void blocksWriteTransactionWhenUsageExceedsFreeLimit() {
        when(databaseUsagePort.databaseBytes()).thenReturn(400L * 1024 * 1024);

        assertThatThrownBy(probeService::writeSomething)
                .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void allowsWriteTransactionWhenUnderLimit() {
        when(databaseUsagePort.databaseBytes()).thenReturn(10L);

        assertThat(probeService.writeSomething()).isEqualTo("done");
    }

    @Test
    void readOnlyTransactionIsNeverBlocked() {
        when(databaseUsagePort.databaseBytes()).thenReturn(400L * 1024 * 1024);

        assertThat(probeService.readSomething()).isEqualTo("done");
    }
}
