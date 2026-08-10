package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;

public record LeaveGrantEmployee(Long userId, LocalDate joinedDate) {
}
