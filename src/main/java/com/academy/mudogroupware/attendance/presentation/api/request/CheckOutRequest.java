package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckOutRequest(
        @NotNull(message = "퇴근 유형은 필수입니다.")
        ClockOutType clockOutType,
        @Size(max = 255, message = "퇴근 메모는 255자 이하여야 합니다.")
        String clockOutNote
) {
    public CheckOutCommand toCommand(Long userId, Long academyId, String detectedIpAddress) {
        return new CheckOutCommand(
                userId, academyId, detectedIpAddress, clockOutType, clockOutNote);
    }
}
