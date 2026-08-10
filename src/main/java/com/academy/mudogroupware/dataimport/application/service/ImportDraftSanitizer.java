package com.academy.mudogroupware.dataimport.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportEnrollmentCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImportDraftSanitizer {

    private final ImportDraftValidator validator;

    public ImportDraft sanitize(ImportDraft draft) {
        ImportDraft safeDraft = draft != null ? draft : ImportDraft.empty();
        List<ImportStudentCandidate> students = sanitizeStudents(safeDraft.students());
        List<ImportLectureCandidate> lectures = sanitizeLectures(safeDraft.lectures());
        List<ImportEnrollmentCandidate> enrollments = sanitizeEnrollments(safeDraft.enrollments(), students,
                lectures);
        return new ImportDraft(students, lectures, enrollments);
    }

    private List<ImportStudentCandidate> sanitizeStudents(List<ImportStudentCandidate> candidates) {
        List<ImportStudentCandidate> sanitized = new ArrayList<>();
        for (ImportStudentCandidate candidate : candidates) {
            ImportDraftValidator.Result validation = validator.validateStudent(candidate.name(), candidate.grade());
            sanitized.add(new ImportStudentCandidate(
                    candidate.rowId(),
                    candidate.selected() && validation.status() == ImportRowStatus.READY,
                    validation.status(),
                    candidate.name(),
                    candidate.grade(),
                    candidate.school(),
                    candidate.phone(),
                    candidate.parentPhone(),
                    candidate.note(),
                    validation.messages()));
        }
        return sanitized;
    }

    private List<ImportLectureCandidate> sanitizeLectures(List<ImportLectureCandidate> candidates) {
        List<ImportLectureCandidate> sanitized = new ArrayList<>();
        for (ImportLectureCandidate candidate : candidates) {
            ImportDraftValidator.Result validation = validator.validateLecture(
                    candidate.name(),
                    candidate.grade(),
                    candidate.termName(),
                    candidate.subjectName(),
                    candidate.teacherId(),
                    candidate.teacherName(),
                    candidate.classroomName(),
                    candidate.schedules(),
                    List.of());
            sanitized.add(new ImportLectureCandidate(
                    candidate.rowId(),
                    candidate.selected() && validation.status() == ImportRowStatus.READY,
                    validation.status(),
                    candidate.name(),
                    candidate.grade(),
                    candidate.termName(),
                    candidate.subjectName(),
                    candidate.teacherId(),
                    candidate.teacherName(),
                    candidate.classroomName(),
                    candidate.feeType(),
                    candidate.feeAmount(),
                    candidate.schedules(),
                    validation.messages()));
        }
        return sanitized;
    }

    private List<ImportEnrollmentCandidate> sanitizeEnrollments(List<ImportEnrollmentCandidate> candidates,
                                                                List<ImportStudentCandidate> students,
                                                                List<ImportLectureCandidate> lectures) {
        CandidateIndex studentIndex = buildStudentIndex(students);
        CandidateIndex lectureIndex = buildLectureIndex(lectures);
        List<ImportEnrollmentCandidate> sanitized = new ArrayList<>();
        for (ImportEnrollmentCandidate candidate : candidates) {
            ImportDraftValidator.Result validation = validator.validateEnrollment(candidate.studentName(),
                    candidate.lectureName());
            List<String> messages = new ArrayList<>(validation.messages());
            ImportRowStatus status = validation.status();
            String studentRowId = resolveStudentRowId(studentIndex, candidate);
            String lectureRowId = resolveLectureRowId(lectureIndex, candidate);

            if (status == ImportRowStatus.READY && studentRowId == null) {
                status = ImportRowStatus.NEEDS_REVIEW;
                messages.add("학생 후보 확인이 필요합니다.");
            }
            if (status == ImportRowStatus.READY && lectureRowId == null) {
                status = ImportRowStatus.NEEDS_REVIEW;
                messages.add("강의 후보 확인이 필요합니다.");
            }

            sanitized.add(new ImportEnrollmentCandidate(
                    candidate.rowId(),
                    candidate.selected() && status == ImportRowStatus.READY,
                    status,
                    studentRowId,
                    lectureRowId,
                    candidate.studentName(),
                    candidate.studentPhone(),
                    candidate.lectureName(),
                    candidate.teacherName(),
                    messages));
        }
        return sanitized;
    }

    private CandidateIndex buildStudentIndex(List<ImportStudentCandidate> students) {
        CandidateIndex index = new CandidateIndex();
        for (ImportStudentCandidate candidate : students) {
            if (candidate.status() != ImportRowStatus.READY) {
                continue;
            }
            index.addRowId(candidate.rowId());
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
            index.addRowId(candidate.rowId());
            index.add(matchKey(candidate.name()), candidate.rowId());
            if (candidate.teacherName() != null) {
                index.add(matchKey(candidate.name(), candidate.teacherName()), candidate.rowId());
            }
        }
        return index;
    }

    private String resolveStudentRowId(CandidateIndex index, ImportEnrollmentCandidate candidate) {
        if (index.containsRowId(candidate.studentRowId())) {
            return candidate.studentRowId();
        }
        if (candidate.studentPhone() != null) {
            return index.unique(matchKey(candidate.studentName(), candidate.studentPhone()));
        }
        return index.unique(matchKey(candidate.studentName()));
    }

    private String resolveLectureRowId(CandidateIndex index, ImportEnrollmentCandidate candidate) {
        if (index.containsRowId(candidate.lectureRowId())) {
            return candidate.lectureRowId();
        }
        if (candidate.teacherName() != null) {
            return index.unique(matchKey(candidate.lectureName(), candidate.teacherName()));
        }
        return index.unique(matchKey(candidate.lectureName()));
    }

    private String matchKey(String... parts) {
        return Arrays.stream(parts)
                .map(part -> part == null ? "" : part.trim().toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + "\u0001" + right)
                .orElse("");
    }

    private static final class CandidateIndex {
        private final Map<String, List<String>> rowIdsByKey = new HashMap<>();
        private final List<String> readyRowIds = new ArrayList<>();

        private void add(String key, String rowId) {
            rowIdsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rowId);
        }

        private void addRowId(String rowId) {
            readyRowIds.add(rowId);
        }

        private boolean containsRowId(String rowId) {
            return rowId != null && readyRowIds.contains(rowId);
        }

        private String unique(String key) {
            List<String> rowIds = rowIdsByKey.getOrDefault(key, List.of());
            return rowIds.size() == 1 ? rowIds.get(0) : null;
        }
    }
}
