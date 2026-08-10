package com.academy.mudogroupware.attendance.application.command;

public record CheckInCommand(
        Long userId,
        String detectedIpAddress,
        String clockInNote
) {
}
