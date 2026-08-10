package com.academy.mudogroupware.dataimport.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.dataimport.domain.model.ImportLectureSchedule;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

@Component
public class ImportDraftValidator {

    public Result validateStudent(String name, StudentGrade grade) {
        List<String> messages = new ArrayList<>();
        if (name == null || name.isBlank()) {
            messages.add("학생 이름은 필수입니다.");
        }
        if (grade == null) {
            messages.add("학생 학년은 필수입니다.");
        }
        return messages.isEmpty()
                ? new Result(ImportRowStatus.READY, List.of())
                : new Result(ImportRowStatus.ERROR, messages);
    }

    public Result validateLecture(String name, Grade grade, String termName, String subjectName, Long teacherId,
                                  String teacherName, String classroomName, List<ImportLectureSchedule> schedules) {
        List<String> messages = new ArrayList<>();
        if (name == null || name.isBlank()) {
            messages.add("강의명은 필수입니다.");
        }
        if (grade == null) {
            messages.add("강의 학년은 필수입니다.");
        }
        if (termName == null || termName.isBlank()) {
            messages.add("학기명은 필수입니다.");
        }
        if (subjectName == null || subjectName.isBlank()) {
            messages.add("과목명은 필수입니다.");
        }
        if (classroomName == null || classroomName.isBlank()) {
            messages.add("교실명은 필수입니다.");
        }
        if (schedules == null || schedules.isEmpty()) {
            messages.add("강의 일정은 필수입니다.");
        }
        if (!messages.isEmpty()) {
            return new Result(ImportRowStatus.ERROR, messages);
        }
        if (teacherId == null) {
            return new Result(ImportRowStatus.NEEDS_REVIEW,
                    teacherName == null || teacherName.isBlank()
                            ? List.of("강사 ID는 필수입니다.")
                            : List.of("강사 ID 확인이 필요합니다."));
        }
        return new Result(ImportRowStatus.READY, List.of());
    }

    public Result validateEnrollment(String studentName, String lectureName) {
        List<String> messages = new ArrayList<>();
        if (studentName == null || studentName.isBlank()) {
            messages.add("학생명은 필수입니다.");
        }
        if (lectureName == null || lectureName.isBlank()) {
            messages.add("강의명은 필수입니다.");
        }
        return messages.isEmpty()
                ? new Result(ImportRowStatus.READY, List.of())
                : new Result(ImportRowStatus.ERROR, messages);
    }

    public record Result(ImportRowStatus status, List<String> messages) {

        public Result {
            messages = messages != null ? List.copyOf(messages) : List.of();
        }
    }
}
