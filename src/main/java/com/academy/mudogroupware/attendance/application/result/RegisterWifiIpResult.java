package com.academy.mudogroupware.attendance.application.result;

import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

public record RegisterWifiIpResult(
        Long wifiIpId,
        String ipAddress,
        String note,
        LocalDateTime createdAt
) {
    public static RegisterWifiIpResult from(AcademyWifiIp wifiIp) {
        return new RegisterWifiIpResult(
                wifiIp.getId(),
                wifiIp.getIpAddress(),
                wifiIp.getNote(),
                wifiIp.getCreatedAt());
    }
}
