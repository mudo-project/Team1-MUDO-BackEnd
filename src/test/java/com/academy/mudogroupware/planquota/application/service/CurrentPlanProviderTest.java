package com.academy.mudogroupware.planquota.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.planquota.application.port.InstancePlanConfigPort;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;

class CurrentPlanProviderTest {

    private final InstancePlanConfigPort instancePlanConfigPort = mock(InstancePlanConfigPort.class);
    private final CurrentPlanProvider provider = new CurrentPlanProvider(instancePlanConfigPort);

    @Test
    void parsesKnownPlanValue() {
        when(instancePlanConfigPort.rawPlan()).thenReturn("PAID");

        assertThat(provider.currentPlan()).isEqualTo(Plan.PAID);
        assertThat(provider.currentLimits()).isEqualTo(PlanLimits.of(Plan.PAID));
    }

    @Test
    void unknownPlanValueFallsBackToFree() {
        when(instancePlanConfigPort.rawPlan()).thenReturn("local");

        assertThat(provider.currentPlan()).isEqualTo(Plan.FREE);
    }

    @Test
    void parsingIsCaseInsensitive() {
        when(instancePlanConfigPort.rawPlan()).thenReturn("free");

        assertThat(provider.currentPlan()).isEqualTo(Plan.FREE);
    }
}
