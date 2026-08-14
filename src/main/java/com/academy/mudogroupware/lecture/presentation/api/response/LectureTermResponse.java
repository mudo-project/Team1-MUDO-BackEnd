package com.academy.mudogroupware.lecture.presentation.api.response;

import com.academy.mudogroupware.lecture.application.query.TermOptionView;

public record LectureTermResponse(
        Long termId,
        String termName
) {

    public static LectureTermResponse from(TermOptionView view) {
        return new LectureTermResponse(view.termId(), view.termName());
    }
}
