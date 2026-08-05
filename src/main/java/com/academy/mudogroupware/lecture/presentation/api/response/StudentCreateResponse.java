package com.academy.mudogroupware.lecture.presentation.api.response;

public record StudentCreateResponse(
        Long studentId
) {

    public static StudentCreateResponse from(Long studentId) {
        return new StudentCreateResponse(studentId);
    }
}
