package com.academy.mudogroupware.lecture.application.port;

public record EnrolledStudentInfo(
        Long studentId,
        String name,
        String grade,
        String parentPhone
) {
}
