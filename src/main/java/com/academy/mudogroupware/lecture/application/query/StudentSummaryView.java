package com.academy.mudogroupware.lecture.application.query;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public record StudentSummaryView(
        Long id,
        String name,
        Grade grade
) {
}
