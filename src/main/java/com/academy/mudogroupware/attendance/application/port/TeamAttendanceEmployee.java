package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDateTime;

public record TeamAttendanceEmployee(
        Long userId,
        String name,
        LocalDateTime clockInAt
) {
}
