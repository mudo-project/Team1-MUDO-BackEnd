package com.academy.mudogroupware.attendance.application.command;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubmitLeaveRequestCommand(
        Long documentId,
        Long userId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime submittedAt
) {
}
