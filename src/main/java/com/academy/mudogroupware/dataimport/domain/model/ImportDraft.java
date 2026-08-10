package com.academy.mudogroupware.dataimport.domain.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ImportDraft(
        List<ImportStudentCandidate> students,
        List<ImportLectureCandidate> lectures,
        List<ImportEnrollmentCandidate> enrollments
) {

    public ImportDraft {
        students = students != null ? List.copyOf(students) : List.of();
        lectures = lectures != null ? List.copyOf(lectures) : List.of();
        enrollments = enrollments != null ? List.copyOf(enrollments) : List.of();
    }

    public static ImportDraft empty() {
        return new ImportDraft(List.of(), List.of(), List.of());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return students.isEmpty() && lectures.isEmpty() && enrollments.isEmpty();
    }
}
