package com.academy.mudogroupware.student.application.command;

import com.academy.mudogroupware.student.domain.model.StudentGrade;

public record CreateStudentCommand(
        String name,
        StudentGrade grade,
        String school,
        String phone,
        String parentPhone,
        String note
) {
}
