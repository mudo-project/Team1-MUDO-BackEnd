package com.academy.mudogroupware.dataimport.presentation.api.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportEnrollmentCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureSchedule;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

public record ImportDraftResponse(
        List<StudentCandidateResponse> students,
        List<LectureCandidateResponse> lectures,
        List<EnrollmentCandidateResponse> enrollments
) {

    public static ImportDraftResponse from(ImportDraft draft) {
        return new ImportDraftResponse(
                draft.students().stream().map(StudentCandidateResponse::from).toList(),
                draft.lectures().stream().map(LectureCandidateResponse::from).toList(),
                draft.enrollments().stream().map(EnrollmentCandidateResponse::from).toList());
    }

    public record StudentCandidateResponse(
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
        static StudentCandidateResponse from(ImportStudentCandidate candidate) {
            return new StudentCandidateResponse(candidate.rowId(), candidate.selected(), candidate.status(),
                    candidate.name(), candidate.grade(), candidate.school(), candidate.phone(),
                    candidate.parentPhone(), candidate.note(), candidate.messages());
        }
    }

    public record LectureCandidateResponse(
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
            List<ScheduleResponse> schedules,
            List<String> messages
    ) {
        static LectureCandidateResponse from(ImportLectureCandidate candidate) {
            return new LectureCandidateResponse(candidate.rowId(), candidate.selected(), candidate.status(),
                    candidate.name(), candidate.grade(), candidate.termName(), candidate.subjectName(),
                    candidate.teacherId(), candidate.teacherName(), candidate.classroomName(), candidate.feeType(),
                    candidate.feeAmount(), candidate.schedules().stream().map(ScheduleResponse::from).toList(),
                    candidate.messages());
        }
    }

    public record ScheduleResponse(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        static ScheduleResponse from(ImportLectureSchedule schedule) {
            return new ScheduleResponse(schedule.dayOfWeek(), schedule.startTime(), schedule.endTime());
        }
    }

    public record EnrollmentCandidateResponse(
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
        static EnrollmentCandidateResponse from(ImportEnrollmentCandidate candidate) {
            return new EnrollmentCandidateResponse(candidate.rowId(), candidate.selected(), candidate.status(),
                    candidate.studentRowId(), candidate.lectureRowId(), candidate.studentName(),
                    candidate.studentPhone(), candidate.lectureName(), candidate.teacherName(), candidate.messages());
        }
    }
}
