package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterWifiIpRequest(
        @Schema(
                description = "현재 접속 IP 조회 API에서 확인한 IP 주소",
                example = "203.0.113.10")
        @NotBlank(message = "확인한 IP 주소는 필수입니다.")
        @Size(max = 45, message = "IP 주소는 45자 이하여야 합니다.")
        String confirmedIpAddress,

        @Schema(
                description = "등록할 Wi-Fi IP를 구분하기 위한 메모",
                example = "강남점 메인 Wi-Fi")
        @Size(max = 100, message = "메모는 100자 이하여야 합니다.")
        String note
) {
    public RegisterWifiIpCommand toCommand(Long requesterId, String detectedIpAddress) {
        return new RegisterWifiIpCommand(
                requesterId, confirmedIpAddress, detectedIpAddress, note);
    }
}
