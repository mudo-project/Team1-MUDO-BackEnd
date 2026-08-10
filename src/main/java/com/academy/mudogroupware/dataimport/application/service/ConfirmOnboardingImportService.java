package com.academy.mudogroupware.dataimport.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.dataimport.application.usecase.ConfirmOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.DataImportStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportEnrollmentCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;
import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;
import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.usecase.CreateLectureUseCase;
import com.academy.mudogroupware.student.application.command.CreateStudentCommand;
import com.academy.mudogroupware.student.application.command.EnrollStudentCommand;
import com.academy.mudogroupware.student.application.usecase.CreateStudentUseCase;
import com.academy.mudogroupware.student.application.usecase.EnrollStudentUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmOnboardingImportService implements ConfirmOnboardingImportUseCase {

    private final DataImportJobRepository dataImportJobRepository;
    private final CreateStudentUseCase createStudentUseCase;
    private final CreateLectureUseCase createLectureUseCase;
    private final EnrollStudentUseCase enrollStudentUseCase;
    private final Clock clock;

    @Override
    public ImportResult confirm(Long academyId, Long importId, Long requesterId) {
        DataImportJob job = loadScopedDraftJob(academyId, importId);
        ImportDraft draft = job.getDraft();
        assertSelectedRowsReady(draft);

        Map<String, Long> createdStudentIds = createStudents(academyId, draft.students());
        Map<String, Long> createdLectureIds = createLectures(academyId, requesterId, draft.lectures());
        int createdEnrollments = createEnrollments(academyId, draft.enrollments(), createdStudentIds,
                createdLectureIds);

        int selectedRows = selectedCount(draft);
        int totalRows = draft.students().size() + draft.lectures().size() + draft.enrollments().size();
        ImportResult result = new ImportResult(createdStudentIds.size(), createdLectureIds.size(), createdEnrollments,
                totalRows - selectedRows, 0);
        job.confirm(result, LocalDateTime.now(clock));
        dataImportJobRepository.save(job);
        return result;
    }

    private DataImportJob loadScopedDraftJob(Long academyId, Long importId) {
        DataImportJob job = dataImportJobRepository.findById(importId)
                .orElseThrow(() -> new DataImportException(DataImportErrorCode.IMPORT_NOT_FOUND));
        if (!job.getAcademyId().equals(academyId)) {
            throw new DataImportException(DataImportErrorCode.IMPORT_ACCESS_DENIED);
        }
        if (job.getStatus() != DataImportStatus.DRAFT) {
            throw new DataImportException(DataImportErrorCode.IMPORT_ALREADY_CONFIRMED);
        }
        return job;
    }

    private void assertSelectedRowsReady(ImportDraft draft) {
        boolean hasInvalidSelectedRow = draft.students().stream().anyMatch(this::selectedButNotReady)
                || draft.lectures().stream().anyMatch(this::selectedButNotReady)
                || draft.enrollments().stream().anyMatch(this::selectedButNotReady);
        if (hasInvalidSelectedRow) {
            throw new DataImportException(DataImportErrorCode.SELECTED_ROW_NOT_READY);
        }
    }

    private boolean selectedButNotReady(ImportStudentCandidate candidate) {
        return candidate.selected() && candidate.status() != ImportRowStatus.READY;
    }

    private boolean selectedButNotReady(ImportLectureCandidate candidate) {
        return candidate.selected() && candidate.status() != ImportRowStatus.READY;
    }

    private boolean selectedButNotReady(ImportEnrollmentCandidate candidate) {
        return candidate.selected() && candidate.status() != ImportRowStatus.READY;
    }

    private Map<String, Long> createStudents(Long academyId, List<ImportStudentCandidate> candidates) {
        Map<String, Long> ids = new HashMap<>();
        for (ImportStudentCandidate candidate : candidates) {
            if (!candidate.selected()) {
                continue;
            }
            Long studentId = createStudentUseCase.createStudent(new CreateStudentCommand(
                    academyId,
                    candidate.name(),
                    candidate.grade(),
                    candidate.school(),
                    candidate.phone(),
                    candidate.parentPhone(),
                    candidate.note()));
            ids.put(candidate.rowId(), studentId);
        }
        return ids;
    }

    private Map<String, Long> createLectures(Long academyId, Long requesterId, List<ImportLectureCandidate> candidates) {
        Map<String, Long> ids = new HashMap<>();
        for (ImportLectureCandidate candidate : candidates) {
            if (!candidate.selected()) {
                continue;
            }
            List<ScheduleInput> schedules = candidate.schedules().stream()
                    .map(schedule -> new ScheduleInput(schedule.dayOfWeek(), schedule.startTime(), schedule.endTime()))
                    .toList();
            Long lectureId = createLectureUseCase.createLecture(new CreateLectureCommand(
                    academyId,
                    candidate.name(),
                    candidate.grade(),
                    candidate.termName(),
                    candidate.subjectName(),
                    candidate.teacherId(),
                    candidate.classroomName(),
                    candidate.feeType(),
                    candidate.feeAmount(),
                    schedules,
                    requesterId));
            ids.put(candidate.rowId(), lectureId);
        }
        return ids;
    }

    private int createEnrollments(Long academyId, List<ImportEnrollmentCandidate> candidates,
                                  Map<String, Long> studentIds, Map<String, Long> lectureIds) {
        int created = 0;
        for (ImportEnrollmentCandidate candidate : candidates) {
            if (!candidate.selected()) {
                continue;
            }
            Long studentId = studentIds.get(candidate.studentRowId());
            Long lectureId = lectureIds.get(candidate.lectureRowId());
            if (studentId == null || lectureId == null) {
                throw new DataImportException(DataImportErrorCode.ENROLLMENT_TARGET_NOT_FOUND);
            }
            enrollStudentUseCase.enroll(new EnrollStudentCommand(academyId, studentId, lectureId));
            created++;
        }
        return created;
    }

    private int selectedCount(ImportDraft draft) {
        return (int) (draft.students().stream().filter(ImportStudentCandidate::selected).count()
                + draft.lectures().stream().filter(ImportLectureCandidate::selected).count()
                + draft.enrollments().stream().filter(ImportEnrollmentCandidate::selected).count());
    }
}
