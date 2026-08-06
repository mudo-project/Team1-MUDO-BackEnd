package com.academy.mudogroupware.rollcall.application.query;

import java.time.LocalDate;
import java.util.List;

public record RosterView(
        Long lectureId,
        String lectureName,
        LocalDate date,
        List<RosterEntryView> entries,
        RosterSummaryView summary
) {
}
