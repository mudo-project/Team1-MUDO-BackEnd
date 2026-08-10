package com.academy.mudogroupware.dataimport.domain.model;

import java.util.List;

public record ImportEnrollmentCandidate(
        String rowId,
        boolean selected,
        ImportRowStatus status,
        String studentRowId,
        String lectureRowId,
        String studentName,
        String studentPhone,
        String lectureName,
        String teacherName,
        List<String> messages
) {

    public ImportEnrollmentCandidate {
        messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
