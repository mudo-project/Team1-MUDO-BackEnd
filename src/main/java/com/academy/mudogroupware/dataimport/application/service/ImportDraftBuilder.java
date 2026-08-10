package com.academy.mudogroupware.dataimport.application.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                ScheduleBuildResult scheduleResult = buildSchedules(row);
                ImportDraftValidator.Result validation = validator.validateLecture(
                        name, grade, termName, subjectName, teacherId, teacherName, classroomName,
                        scheduleResult.schedules(), scheduleResult.messages());

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
                        scheduleResult.schedules(),
                        validation.messages()));
            }
        }
        return candidates;
    }

    private List<ImportEnrollmentCandidate> buildEnrollments(List<ParsedImportSheet> sheets,
                                                             List<ImportStudentCandidate> students,
                                                             List<ImportLectureCandidate> lectures) {
        List<ImportEnrollmentCandidate> candidates = new ArrayList<>();
        CandidateIndex studentIndex = buildStudentIndex(students);
        CandidateIndex lectureIndex = buildLectureIndex(lectures);
        int order = 1;
        for (ParsedImportSheet sheet : sheetsByRole(sheets, ImportFileRole.ENROLLMENT)) {
            for (ParsedImportRow row : sheet.rows()) {
                String studentName = normalizer.text(row, "학생명", "studentName");
                String studentPhone = normalizer.text(row, "학생연락처", "studentPhone", "연락처");
                String lectureName = normalizer.text(row, "강의명", "lectureName");
                String teacherName = normalizer.text(row, "강사명", "teacherName");
                String studentRowId = findStudentRowId(studentIndex, studentName, studentPhone);
                String lectureRowId = findLectureRowId(lectureIndex, lectureName, teacherName);
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

    private ScheduleBuildResult buildSchedules(ParsedImportRow row) {
        List<ImportLectureSchedule> schedules = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (int order = 1; order <= 7; order++) {
            ScheduleBuildResult result = buildSingleSchedule(row,
                    List.of("요일" + order, "day" + order, "dayOfWeek" + order),
                    List.of("시작" + order, "start" + order, "startTime" + order),
                    List.of("종료" + order, "end" + order, "endTime" + order));
            schedules.addAll(result.schedules());
            messages.addAll(result.messages());
        }

        if (!schedules.isEmpty() || !messages.isEmpty()) {
            return new ScheduleBuildResult(schedules, messages);
        }
        return buildSingleSchedule(row,
                List.of("요일", "day", "dayOfWeek"),
                List.of("시작", "start", "startTime"),
                List.of("종료", "end", "endTime"));
    }

    private ScheduleBuildResult buildSingleSchedule(ParsedImportRow row, List<String> dayAliases,
                                                    List<String> startAliases, List<String> endAliases) {
        String dayValue = normalizer.text(row, dayAliases.toArray(String[]::new));
        String startValue = normalizer.text(row, startAliases.toArray(String[]::new));
        String endValue = normalizer.text(row, endAliases.toArray(String[]::new));
        if (dayValue == null && startValue == null && endValue == null) {
            return new ScheduleBuildResult(List.of(), List.of());
        }

        List<String> dayParts = splitScheduleValues(dayValue);
        List<String> startParts = splitScheduleValues(startValue);
        List<String> endParts = splitScheduleValues(endValue);
        int count = Math.max(dayParts.size(), Math.max(startParts.size(), endParts.size()));
        List<ImportLectureSchedule> schedules = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            String dayPart = valueAt(dayParts, index);
            String startPart = valueAt(startParts, index);
            String endPart = valueAt(endParts, index);
            DayOfWeek dayOfWeek = dayPart != null ? normalizer.dayOfWeekValue(dayPart) : null;
            LocalTime startTime = startPart != null ? normalizer.timeValue(startPart) : null;
            LocalTime endTime = endPart != null ? normalizer.timeValue(endPart) : null;

            if (dayPart != null && dayOfWeek == null) {
                messages.add("요일 형식이 올바르지 않습니다: " + dayPart);
            }
            if (startPart != null && startTime == null) {
                messages.add("시작 시간 형식이 올바르지 않습니다: " + startPart);
            }
            if (endPart != null && endTime == null) {
                messages.add("종료 시간 형식이 올바르지 않습니다: " + endPart);
            }
            if (dayOfWeek != null && startTime != null && endTime != null) {
                schedules.add(new ImportLectureSchedule(dayOfWeek, startTime, endTime));
            }
        }
        return new ScheduleBuildResult(schedules, messages);
    }

    private List<ParsedImportSheet> sheetsByRole(List<ParsedImportSheet> sheets, ImportFileRole role) {
        return sheets.stream()
                .filter(sheet -> sheet.role() == role)
                .toList();
    }

    private CandidateIndex buildStudentIndex(List<ImportStudentCandidate> students) {
        CandidateIndex index = new CandidateIndex();
        for (ImportStudentCandidate candidate : students) {
            if (candidate.status() != ImportRowStatus.READY) {
                continue;
            }
            index.add(matchKey(candidate.name()), candidate.rowId());
            if (candidate.phone() != null) {
                index.add(matchKey(candidate.name(), candidate.phone()), candidate.rowId());
            }
        }
        return index;
    }

    private CandidateIndex buildLectureIndex(List<ImportLectureCandidate> lectures) {
        CandidateIndex index = new CandidateIndex();
        for (ImportLectureCandidate candidate : lectures) {
            if (candidate.status() != ImportRowStatus.READY) {
                continue;
            }
            index.add(matchKey(candidate.name()), candidate.rowId());
            if (candidate.teacherName() != null) {
                index.add(matchKey(candidate.name(), candidate.teacherName()), candidate.rowId());
            }
        }
        return index;
    }

    private String findStudentRowId(CandidateIndex index, String studentName, String studentPhone) {
        return studentPhone != null ? index.unique(matchKey(studentName, studentPhone)) : index.unique(matchKey(studentName));
    }

    private String findLectureRowId(CandidateIndex index, String lectureName, String teacherName) {
        return teacherName != null ? index.unique(matchKey(lectureName, teacherName)) : index.unique(matchKey(lectureName));
    }

    private List<String> splitScheduleValues(String value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(value.split("[,;/|]"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private String valueAt(List<String> values, int index) {
        if (values.isEmpty()) {
            return null;
        }
        return values.size() == 1 ? values.get(0) : index < values.size() ? values.get(index) : null;
    }

    private String matchKey(String... parts) {
        return Arrays.stream(parts)
                .map(part -> part == null ? "" : part.trim().toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + "\u0001" + right)
                .orElse("");
    }

    private record ScheduleBuildResult(List<ImportLectureSchedule> schedules, List<String> messages) {

        private ScheduleBuildResult {
            schedules = schedules != null ? List.copyOf(schedules) : List.of();
            messages = messages != null ? List.copyOf(messages) : List.of();
        }
    }

    private static final class CandidateIndex {
        private final Map<String, List<String>> rowIdsByKey = new HashMap<>();

        private void add(String key, String rowId) {
            rowIdsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rowId);
        }

        private String unique(String key) {
            List<String> rowIds = rowIdsByKey.getOrDefault(key, List.of());
            return rowIds.size() == 1 ? rowIds.get(0) : null;
        }
    }
}
