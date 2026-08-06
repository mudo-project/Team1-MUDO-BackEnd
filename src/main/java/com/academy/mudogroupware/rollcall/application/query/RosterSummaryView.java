package com.academy.mudogroupware.rollcall.application.query;

public record RosterSummaryView(
        int total,
        int present,
        int absent,
        int late,
        int online,
        int etc
) {
}
