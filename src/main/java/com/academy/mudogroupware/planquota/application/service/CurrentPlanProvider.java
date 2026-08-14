package com.academy.mudogroupware.planquota.application.service;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentPlanProvider {

    private final InstanceMetadataProperties instanceMetadataProperties;

    public Plan currentPlan() {
        try {
            return Plan.valueOf(instanceMetadataProperties.getPlan().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Plan.FREE;
        }
    }

    public PlanLimits currentLimits() {
        return PlanLimits.of(currentPlan());
    }
}
