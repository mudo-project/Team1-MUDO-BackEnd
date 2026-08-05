package com.academy.mudogroupware.rollcall.application.command;

import java.time.LocalDate;
import java.util.List;

public record SaveAttendanceEntriesCommand(
        Long lectureId,
        Long academyId,
        LocalDate date,
        List<AttendanceEntryInput> entries
) {
}
