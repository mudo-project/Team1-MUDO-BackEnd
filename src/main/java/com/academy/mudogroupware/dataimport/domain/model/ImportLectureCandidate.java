package com.academy.mudogroupware.dataimport.domain.model;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record ImportLectureCandidate(
        String rowId,
        boolean selected,
        ImportRowStatus status,
        String name,
        Grade grade,
        String termName,
        String subjectName,
        Long teacherId,
        String teacherName,
        String classroomName,
        FeeType feeType,
        Integer feeAmount,
        List<ImportLectureSchedule> schedules,
        List<String> messages
) {

    public ImportLectureCandidate {
        schedules = schedules != null ? List.copyOf(schedules) : List.of();
        messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
