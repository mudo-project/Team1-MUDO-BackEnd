package com.academy.mudogroupware.attendance.application.command;

public record CheckOutCommand(
        Long userId,
        Long academyId,
        String detectedIpAddress,
        String clockOutNote
) {
}
