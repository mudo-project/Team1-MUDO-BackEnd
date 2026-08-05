package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterWifiIpRequest(
        @NotBlank(message = "확인한 IP 주소는 필수입니다.")
        @Size(max = 45, message = "IP 주소는 45자 이하여야 합니다.")
        String confirmedIpAddress,

        @Size(max = 100, message = "메모는 100자 이하여야 합니다.")
        String note
) {
    public RegisterWifiIpCommand toCommand(Long requesterId, String detectedIpAddress) {
        return new RegisterWifiIpCommand(
                requesterId, confirmedIpAddress, detectedIpAddress, note);
    }
}
