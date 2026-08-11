package com.academy.mudogroupware.dataimport.domain.model;

public record ImportResult(
        int createdStudents,
        int createdLectures,
        int createdEnrollments,
        int skippedRows,
        int failedRows
) {
}
