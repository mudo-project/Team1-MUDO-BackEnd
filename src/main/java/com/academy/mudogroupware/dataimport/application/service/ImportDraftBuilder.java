package com.academy.mudogroupware.dataimport.application.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportEnrollmentCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureSchedule;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImportDraftBuilder {

    private final ImportValueNormalizer normalizer;
    private final ImportDraftValidator validator;

    public ImportDraft build(List<ParsedImportSheet> sheets) {
        List<ParsedImportSheet> safeSheets = sheets != null ? sheets : List.of();
        List<ImportStudentCandidate> students = buildStudents(safeSheets);
        List<ImportLectureCandidate> lectures = buildLectures(safeSheets);
        List<ImportEnrollmentCandidate> enrollments = buildEnrollments(safeSheets, students, lectures);
        return new ImportDraft(students, lectures, enrollments);
    }

    private List<ImportStudentCandidate> buildStudents(List<ParsedImportSheet> sheets) {
        List<ImportStudentCandidate> candidates = new ArrayList<>();
        int order = 1;
        for (ParsedImportSheet sheet : sheetsByRole(sheets, ImportFileRole.STUDENT)) {
            for (ParsedImportRow row : sheet.rows()) {
                String name = normalizer.text(row, "이름", "name", "학생명");
                StudentGrade grade = normalizer.studentGrade(row, "학년", "grade");
                String school = normalizer.text(row, "학교", "school");
                String phone = normalizer.text(row, "연락처", "전화번호", "phone");
                String parentPhone = normalizer.text(row, "보호자연락처", "학부모연락처", "parentPhone");
                String note = normalizer.text(row, "메모", "note", "특이사항");
                ImportDraftValidator.Result validation = validator.validateStudent(name, grade);

                candidates.add(new ImportStudentCandidate(
                        "S" + order++,
                        validation.status() == ImportRowStatus.READY,
                        validation.status(),
                        name,
                        grade,
                        school,
                        phone,
                        parentPhone,
                        note,
                        validation.messages()));
            }
        }
        return candidates;
    }

    private List<ImportLectureCandidate> buildLectures(List<ParsedImportSheet> sheets) {
        List<ImportLectureCandidate> candidates = new ArrayList<>();
        int order = 1;
        for (ParsedImportSheet sheet : sheetsByRole(sheets, ImportFileRole.LECTURE)) {
            for (ParsedImportRow row : sheet.rows()) {
                String name = normalizer.text(row, "강의명", "수업명", "name");
                Grade grade = normalizer.lectureGrade(row, "학년", "grade");
                String termName = normalizer.text(row, "학기", "term", "termName");
                String subjectName = normalizer.text(row, "과목", "subject", "subjectName");
                Long teacherId = normalizer.longValue(row, "강사ID", "teacherId");
                String teacherName = normalizer.text(row, "강사명", "teacherName");
                String classroomName = normalizer.text(row, "교실", "classroom", "classroomName");
                FeeType feeType = normalizer.feeType(row, "수강료구분", "feeType");
                Integer feeAmount = normalizer.integerValue(row, "수강료", "가격", "feeAmount");
                List<ImportLectureSchedule> schedules = buildSchedules(row);
                ImportDraftValidator.Result validation = validator.validateLecture(
                        name, grade, termName, subjectName, teacherId, teacherName, classroomName, schedules);

                candidates.add(new ImportLectureCandidate(
                        "L" + order++,
                        validation.status() == ImportRowStatus.READY,
                        validation.status(),
                        name,
                        grade,
                        termName,
                        subjectName,
                        teacherId,
                        teacherName,
                        classroomName,
                        feeType,
                        feeAmount,
                        schedules,
                        validation.messages()));
            }
        }
        return candidates;
    }

    private List<ImportEnrollmentCandidate> buildEnrollments(List<ParsedImportSheet> sheets,
                                                             List<ImportStudentCandidate> students,
                                                             List<ImportLectureCandidate> lectures) {
        List<ImportEnrollmentCandidate> candidates = new ArrayList<>();
        int order = 1;
        for (ParsedImportSheet sheet : sheetsByRole(sheets, ImportFileRole.ENROLLMENT)) {
            for (ParsedImportRow row : sheet.rows()) {
                String studentName = normalizer.text(row, "학생명", "studentName");
                String studentPhone = normalizer.text(row, "학생연락처", "studentPhone", "연락처");
                String lectureName = normalizer.text(row, "강의명", "lectureName");
                String teacherName = normalizer.text(row, "강사명", "teacherName");
                String studentRowId = findStudentRowId(students, studentName, studentPhone);
                String lectureRowId = findLectureRowId(lectures, lectureName, teacherName);
                ImportDraftValidator.Result validation = validator.validateEnrollment(studentName, lectureName);
                List<String> messages = new ArrayList<>(validation.messages());
                ImportRowStatus status = validation.status();
                if (status == ImportRowStatus.READY && studentRowId == null) {
                    status = ImportRowStatus.NEEDS_REVIEW;
                    messages.add("학생 후보 확인이 필요합니다.");
                }
                if (status == ImportRowStatus.READY && lectureRowId == null) {
                    status = ImportRowStatus.NEEDS_REVIEW;
                    messages.add("강의 후보 확인이 필요합니다.");
                }

                candidates.add(new ImportEnrollmentCandidate(
                        "E" + order++,
                        status == ImportRowStatus.READY,
                        status,
                        studentRowId,
                        lectureRowId,
                        studentName,
                        studentPhone,
                        lectureName,
                        teacherName,
                        messages));
            }
        }
        return candidates;
    }

    private List<ImportLectureSchedule> buildSchedules(ParsedImportRow row) {
        DayOfWeek dayOfWeek = normalizer.dayOfWeek(row, "요일", "day", "dayOfWeek");
        LocalTime startTime = normalizer.time(row, "시작", "start", "startTime");
        LocalTime endTime = normalizer.time(row, "종료", "end", "endTime");
        if (dayOfWeek == null || startTime == null || endTime == null) {
            return List.of();
        }
        return List.of(new ImportLectureSchedule(dayOfWeek, startTime, endTime));
    }

    private List<ParsedImportSheet> sheetsByRole(List<ParsedImportSheet> sheets, ImportFileRole role) {
        return sheets.stream()
                .filter(sheet -> sheet.role() == role)
                .toList();
    }

    private String findStudentRowId(List<ImportStudentCandidate> students, String studentName, String studentPhone) {
        List<ImportStudentCandidate> matches = students.stream()
                .filter(candidate -> equalsValue(candidate.name(), studentName))
                .filter(candidate -> studentPhone == null || equalsValue(candidate.phone(), studentPhone))
                .toList();
        return matches.size() == 1 ? matches.get(0).rowId() : null;
    }

    private String findLectureRowId(List<ImportLectureCandidate> lectures, String lectureName, String teacherName) {
        List<ImportLectureCandidate> matches = lectures.stream()
                .filter(candidate -> equalsValue(candidate.name(), lectureName))
                .filter(candidate -> teacherName == null || equalsValue(candidate.teacherName(), teacherName))
                .toList();
        return matches.size() == 1 ? matches.get(0).rowId() : null;
    }

    private boolean equalsValue(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }
}
