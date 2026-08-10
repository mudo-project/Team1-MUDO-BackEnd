package com.academy.mudogroupware.lecture.presentation.api.response;

public record LectureCreateResponse(
        Long lectureId
) {

    public static LectureCreateResponse from(Long lectureId) {
        return new LectureCreateResponse(lectureId);
    }
}
