package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;

public record MyLeaveSummaryView(int totalDays, int usedDays, int pendingDays,
                                 int remainingDays, LocalDate nextGrantDate) {
}
