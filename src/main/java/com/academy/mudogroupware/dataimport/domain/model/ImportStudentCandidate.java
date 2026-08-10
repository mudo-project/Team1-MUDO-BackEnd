package com.academy.mudogroupware.dataimport.domain.model;

import java.util.List;

import com.academy.mudogroupware.student.domain.model.StudentGrade;

public record ImportStudentCandidate(
        String rowId,
        boolean selected,
        ImportRowStatus status,
        String name,
        StudentGrade grade,
        String school,
        String phone,
        String parentPhone,
        String note,
        List<String> messages
) {

    public ImportStudentCandidate {
        messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
