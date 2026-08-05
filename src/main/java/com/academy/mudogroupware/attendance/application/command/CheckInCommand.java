package com.academy.mudogroupware.attendance.application.command;

public record CheckInCommand(
        Long userId,
        Long academyId,
        String detectedIpAddress,
        String clockInNote
) {
}
