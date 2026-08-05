package com.academy.mudogroupware.student.application.query;

import com.academy.mudogroupware.student.domain.model.StudentGrade;

public record StudentSummary(
        Long id,
        String name,
        StudentGrade grade,
        String school,
        String phone,
        String parentPhone,
        int activeEnrollmentCount
) {
}
