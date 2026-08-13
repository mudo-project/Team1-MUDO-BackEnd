package com.academy.mudogroupware.resourceusage.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceUsageResponseCode implements ResponseCode {

    MONTHLY_USAGE_RETRIEVED("RESOURCE_USAGE_200_1", "Resource usage retrieved.");

    private final String code;
    private final String message;
}
