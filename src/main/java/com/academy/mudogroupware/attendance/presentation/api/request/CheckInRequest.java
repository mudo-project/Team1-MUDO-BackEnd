package com.academy.mudogroupware.attendance.presentation.api.request;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @Schema(
                description = "출근 메모. 지각인 경우 사유를 입력합니다.",
                example = "지하철 지연으로 늦었습니다.")
        @Size(max = 255, message = "출근 메모는 255자 이하여야 합니다.")
        String clockInNote
) {
    public CheckInCommand toCommand(Long userId, String detectedIpAddress) {
        return new CheckInCommand(userId, detectedIpAddress, clockInNote);
    }
}
