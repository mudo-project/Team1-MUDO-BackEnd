package com.academy.mudogroupware.planquota.application.service;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.planquota.application.port.InstancePlanConfigPort;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentPlanProvider {

    private final InstancePlanConfigPort instancePlanConfigPort;

    public Plan currentPlan() {
        try {
            return Plan.valueOf(instancePlanConfigPort.rawPlan().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Plan.FREE;
        }
    }

    public PlanLimits currentLimits() {
        return PlanLimits.of(currentPlan());
    }
}
