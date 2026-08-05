package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;

import jakarta.validation.constraints.Size;

public record CheckOutRequest(
        @Size(max = 255, message = "퇴근 메모는 255자 이하여야 합니다.")
        String clockOutNote
) {
    public CheckOutCommand toCommand(Long userId, Long academyId, String detectedIpAddress) {
        return new CheckOutCommand(userId, academyId, detectedIpAddress, clockOutNote);
    }
}
