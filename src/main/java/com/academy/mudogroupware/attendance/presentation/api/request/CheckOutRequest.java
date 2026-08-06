package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckOutRequest(
        @Schema(
                description = "퇴근 유형",
                example = "NORMAL",
                allowableValues = {"NORMAL", "OVERTIME"})
        @NotNull(message = "퇴근 유형은 필수입니다.")
        ClockOutType clockOutType,

        @Schema(
                description = "퇴근 메모. 초과근무인 경우 사유를 입력합니다.",
                example = "학부모 상담으로 초과근무했습니다.")
        @Size(max = 255, message = "퇴근 메모는 255자 이하여야 합니다.")
        String clockOutNote
) {
    public CheckOutCommand toCommand(Long userId, Long academyId, String detectedIpAddress) {
        return new CheckOutCommand(
                userId, academyId, detectedIpAddress, clockOutType, clockOutNote);
    }
}
