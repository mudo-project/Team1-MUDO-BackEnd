package com.academy.mudogroupware.rollcall.application.port;

public record EnrolledStudentRef(
        Long studentId,
        String name,
        String grade,
        String parentPhone
) {
}
