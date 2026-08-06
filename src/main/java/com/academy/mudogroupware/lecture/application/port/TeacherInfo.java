package com.academy.mudogroupware.lecture.application.port;

public record TeacherInfo(
        Long userId,
        String name,
        Long roleId,
        String status
) {
}
