package com.academy.mudogroupware.lecture.presentation.api.response;

import com.academy.mudogroupware.lecture.application.query.StudentSummaryView;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record StudentSummaryResponse(
        Long id,
        String name,
        Grade grade
) {

    public static StudentSummaryResponse from(StudentSummaryView view) {
        return new StudentSummaryResponse(view.id(), view.name(), view.grade());
    }
}
