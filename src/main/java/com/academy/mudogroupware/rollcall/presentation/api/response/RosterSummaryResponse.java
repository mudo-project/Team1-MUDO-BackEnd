package com.academy.mudogroupware.rollcall.presentation.api.response;

import com.academy.mudogroupware.rollcall.application.query.RosterSummaryView;

public record RosterSummaryResponse(
        int total,
        int present,
        int absent,
        int late,
        int online,
        int etc
) {

    public static RosterSummaryResponse from(RosterSummaryView view) {
        return new RosterSummaryResponse(view.total(), view.present(), view.absent(), view.late(), view.online(),
                view.etc());
    }
}
