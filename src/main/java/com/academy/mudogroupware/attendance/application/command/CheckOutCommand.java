package com.academy.mudogroupware.attendance.application.command;

import com.academy.mudogroupware.attendance.domain.model.ClockOutType;

public record CheckOutCommand(
        Long userId,
        Long academyId,
        String detectedIpAddress,
        ClockOutType clockOutType,
        String clockOutNote
) {
}
