package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;

import jakarta.validation.constraints.Size;

public record RegisterWifiIpRequest(
        @Size(max = 100, message = "메모는 100자 이하여야 합니다.")
        String note
) {
    public RegisterWifiIpCommand toCommand(Long requesterId, String ipAddress) {
        return new RegisterWifiIpCommand(requesterId, ipAddress, note);
    }
}
