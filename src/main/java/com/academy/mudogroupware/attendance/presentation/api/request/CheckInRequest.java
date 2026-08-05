package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;

import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @Size(max = 255, message = "출근 메모는 255자 이하여야 합니다.")
        String clockInNote
) {
    public CheckInCommand toCommand(Long userId, Long academyId, String detectedIpAddress) {
        return new CheckInCommand(userId, academyId, detectedIpAddress, clockInNote);
    }
}
