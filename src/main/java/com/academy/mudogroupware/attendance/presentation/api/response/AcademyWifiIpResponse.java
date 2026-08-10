package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcademyWifiIpResponse(
        @Schema(description = "와이파이 IP 식별자", example = "1")
        Long wifiIpId,
        @Schema(description = "출퇴근 허용 공인 IP 주소", example = "203.0.113.10")
        String ipAddress,
        @Schema(description = "IP 구분 메모", example = "본관 와이파이")
        String note,
        @Schema(description = "등록 시각", example = "2026-08-05T10:30:00")
        LocalDateTime createdAt
) {
    public static AcademyWifiIpResponse from(RegisterWifiIpResult result) {
        return new AcademyWifiIpResponse(
                result.wifiIpId(),
                result.ipAddress(),
                result.note(),
                result.createdAt());
    }

    public static AcademyWifiIpResponse from(AcademyWifiIp wifiIp) {
        return new AcademyWifiIpResponse(
                wifiIp.getId(),
                wifiIp.getIpAddress(),
                wifiIp.getNote(),
                wifiIp.getCreatedAt());
    }
}
