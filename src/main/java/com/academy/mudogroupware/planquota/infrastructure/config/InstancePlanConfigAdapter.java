package com.academy.mudogroupware.planquota.infrastructure.config;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import com.academy.mudogroupware.planquota.application.port.InstancePlanConfigPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstancePlanConfigAdapter implements InstancePlanConfigPort {

    private final InstanceMetadataProperties instanceMetadataProperties;

    @Override
    public String rawPlan() {
        return instanceMetadataProperties.getPlan();
    }
}
