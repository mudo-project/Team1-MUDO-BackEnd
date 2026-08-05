package com.academy.mudogroupware.lecture.application.command;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public record RegisterStudentCommand(
        Long academyId,
        String name,
        Grade grade,
        String school,
        String phone,
        String parentPhone,
        String note
) {
}
