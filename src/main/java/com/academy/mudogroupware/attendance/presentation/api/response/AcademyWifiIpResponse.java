package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;

public record AcademyWifiIpResponse(
        Long wifiIpId,
        String ipAddress,
        String note,
        LocalDateTime createdAt
) {
    public static AcademyWifiIpResponse from(RegisterWifiIpResult result) {
        return new AcademyWifiIpResponse(
                result.wifiIpId(),
                result.ipAddress(),
                result.note(),
                result.createdAt());
    }
}
