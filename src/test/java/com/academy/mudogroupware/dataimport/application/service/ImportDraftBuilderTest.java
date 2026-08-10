package com.academy.mudogroupware.dataimport.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

class ImportDraftBuilderTest {

    private final ImportDraftBuilder builder = new ImportDraftBuilder(
            new ImportValueNormalizer(),
            new ImportDraftValidator());

    @Test
    void buildsStudentLectureAndEnrollmentCandidates() {
        ImportDraft draft = builder.build(List.of(studentSheet(), lectureSheet(), enrollmentSheet()));

        assertThat(draft.students()).extracting(ImportStudentCandidate::name).containsExactly("김민수");
        assertThat(draft.students().get(0).grade()).isEqualTo(StudentGrade.HIGH_1);
        assertThat(draft.lectures()).extracting(ImportLectureCandidate::name).containsExactly("고1 수학");
        assertThat(draft.lectures().get(0).grade()).isEqualTo(Grade.HIGH_1);
        assertThat(draft.enrollments()).hasSize(1);
        assertThat(draft.enrollments().get(0).studentName()).isEqualTo("김민수");
    }

    @Test
    void lectureWithoutTeacherIdNeedsReview() {
        ImportDraft draft = builder.build(List.of(lectureSheetWithTeacherNameOnly()));

        assertThat(draft.lectures().get(0).status()).isEqualTo(ImportRowStatus.NEEDS_REVIEW);
        assertThat(draft.lectures().get(0).messages()).contains("강사 ID 확인이 필요합니다.");
    }

    @Test
    void studentWithoutGradeIsError() {
        ParsedImportSheet sheet = new ParsedImportSheet(ImportFileRole.STUDENT, "students.csv",
                List.of(new ParsedImportRow(2, Map.of("이름", "김민수"))));

        ImportDraft draft = builder.build(List.of(sheet));

        assertThat(draft.students().get(0).status()).isEqualTo(ImportRowStatus.ERROR);
        assertThat(draft.students().get(0).messages()).contains("학생 학년은 필수입니다.");
    }

    @Test
    void enrollmentReferencingNonReadyStudentNeedsReview() {
        ParsedImportSheet studentSheet = new ParsedImportSheet(ImportFileRole.STUDENT, "students.csv",
                List.of(new ParsedImportRow(2, Map.of(
                        "이름", "김민수",
                        "연락처", "010-1111-2222"))));

        ImportDraft draft = builder.build(List.of(studentSheet, lectureSheet(), enrollmentSheet()));

        assertThat(draft.students().get(0).status()).isEqualTo(ImportRowStatus.ERROR);
        assertThat(draft.enrollments().get(0).status()).isEqualTo(ImportRowStatus.NEEDS_REVIEW);
        assertThat(draft.enrollments().get(0).selected()).isFalse();
        assertThat(draft.enrollments().get(0).studentRowId()).isNull();
        assertThat(draft.enrollments().get(0).messages()).contains("학생 후보 확인이 필요합니다.");
    }

    @Test
    void enrollmentReferencingNonReadyLectureNeedsReview() {
        ImportDraft draft = builder.build(List.of(studentSheet(), lectureSheetWithTeacherNameOnly(), enrollmentSheet()));

        assertThat(draft.lectures().get(0).status()).isEqualTo(ImportRowStatus.NEEDS_REVIEW);
        assertThat(draft.enrollments().get(0).status()).isEqualTo(ImportRowStatus.NEEDS_REVIEW);
        assertThat(draft.enrollments().get(0).selected()).isFalse();
        assertThat(draft.enrollments().get(0).lectureRowId()).isNull();
        assertThat(draft.enrollments().get(0).messages()).contains("강의 후보 확인이 필요합니다.");
    }

    @Test
    void buildsMultipleSchedulesFromDelimitedDays() {
        ParsedImportSheet sheet = new ParsedImportSheet(ImportFileRole.LECTURE, "lectures.csv", List.of(
                new ParsedImportRow(2, Map.ofEntries(
                        Map.entry("강의명", "고1 수학"),
                        Map.entry("학년", "고1"),
                        Map.entry("학기", "2026 여름"),
                        Map.entry("과목", "수학"),
                        Map.entry("강사ID", "30"),
                        Map.entry("교실", "101호"),
                        Map.entry("요일", "월,수,금"),
                        Map.entry("시작", "15:00"),
                        Map.entry("종료", "17:00")))));

        ImportDraft draft = builder.build(List.of(sheet));

        assertThat(draft.lectures().get(0).schedules()).hasSize(3);
        assertThat(draft.lectures().get(0).status()).isEqualTo(ImportRowStatus.READY);
    }

    @Test
    void lectureWithInvalidScheduleValueReportsSpecificMessage() {
        ParsedImportSheet sheet = new ParsedImportSheet(ImportFileRole.LECTURE, "lectures.csv", List.of(
                new ParsedImportRow(2, Map.ofEntries(
                        Map.entry("강의명", "고1 수학"),
                        Map.entry("학년", "고1"),
                        Map.entry("학기", "2026 여름"),
                        Map.entry("과목", "수학"),
                        Map.entry("강사ID", "30"),
                        Map.entry("교실", "101호"),
                        Map.entry("요일", "월"),
                        Map.entry("시작", "25:00"),
                        Map.entry("종료", "17:00")))));

        ImportDraft draft = builder.build(List.of(sheet));

        assertThat(draft.lectures().get(0).status()).isEqualTo(ImportRowStatus.ERROR);
        assertThat(draft.lectures().get(0).messages()).contains("시작 시간 형식이 올바르지 않습니다: 25:00");
    }

    private ParsedImportSheet studentSheet() {
        return new ParsedImportSheet(ImportFileRole.STUDENT, "students.csv", List.of(
                new ParsedImportRow(2, Map.of(
                        "이름", "김민수",
                        "학년", "고1",
                        "학교", "무도고",
                        "연락처", "010-1111-2222"))));
    }

    private ParsedImportSheet lectureSheet() {
        return new ParsedImportSheet(ImportFileRole.LECTURE, "lectures.csv", List.of(
                new ParsedImportRow(2, Map.ofEntries(
                        Map.entry("강의명", "고1 수학"),
                        Map.entry("학년", "고1"),
                        Map.entry("학기", "2026 여름"),
                        Map.entry("과목", "수학"),
                        Map.entry("강사ID", "30"),
                        Map.entry("교실", "101호"),
                        Map.entry("요일", "월"),
                        Map.entry("시작", "15:00"),
                        Map.entry("종료", "17:00"),
                        Map.entry("수강료구분", "회차별"),
                        Map.entry("수강료", "50000")))));
    }

    private ParsedImportSheet lectureSheetWithTeacherNameOnly() {
        return new ParsedImportSheet(ImportFileRole.LECTURE, "lectures.csv", List.of(
                new ParsedImportRow(2, Map.of(
                        "강의명", "고1 수학",
                        "학년", "고1",
                        "학기", "2026 여름",
                        "과목", "수학",
                        "강사명", "박선생",
                        "교실", "101호",
                        "요일", "월",
                        "시작", "15:00",
                        "종료", "17:00"))));
    }

    private ParsedImportSheet enrollmentSheet() {
        return new ParsedImportSheet(ImportFileRole.ENROLLMENT, "enrollments.csv", List.of(
                new ParsedImportRow(2, Map.of(
                        "학생명", "김민수",
                        "학생연락처", "010-1111-2222",
                        "강의명", "고1 수학"))));
    }
}
