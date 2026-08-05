package com.academy.mudogroupware.attendance.application.command;

public record RegisterWifiIpCommand(
        Long requesterId,
        String confirmedIpAddress,
        String detectedIpAddress,
        String note
) {
}
