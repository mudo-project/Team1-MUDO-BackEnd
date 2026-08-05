package com.academy.mudogroupware.rollcall.presentation.api.response;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.rollcall.application.query.RosterView;

public record RosterResponse(
        Long lectureId,
        String lectureName,
        LocalDate date,
        List<RosterEntryResponse> entries,
        RosterSummaryResponse summary
) {

    public static RosterResponse from(RosterView view) {
        List<RosterEntryResponse> entries = view.entries().stream().map(RosterEntryResponse::from).toList();
        return new RosterResponse(view.lectureId(), view.lectureName(), view.date(), entries,
                RosterSummaryResponse.from(view.summary()));
    }
}
