package com.academy.mudogroupware.planquota.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;

class CurrentPlanProviderTest {

    @Test
    void parsesKnownPlanValue() {
        InstanceMetadataProperties properties = new InstanceMetadataProperties();
        properties.setPlan("PAID");
        CurrentPlanProvider provider = new CurrentPlanProvider(properties);

        assertThat(provider.currentPlan()).isEqualTo(Plan.PAID);
        assertThat(provider.currentLimits()).isEqualTo(PlanLimits.of(Plan.PAID));
    }

    @Test
    void unknownPlanValueFallsBackToFree() {
        InstanceMetadataProperties properties = new InstanceMetadataProperties();
        properties.setPlan("local");
        CurrentPlanProvider provider = new CurrentPlanProvider(properties);

        assertThat(provider.currentPlan()).isEqualTo(Plan.FREE);
    }

    @Test
    void parsingIsCaseInsensitive() {
        InstanceMetadataProperties properties = new InstanceMetadataProperties();
        properties.setPlan("free");
        CurrentPlanProvider provider = new CurrentPlanProvider(properties);

        assertThat(provider.currentPlan()).isEqualTo(Plan.FREE);
    }
}
