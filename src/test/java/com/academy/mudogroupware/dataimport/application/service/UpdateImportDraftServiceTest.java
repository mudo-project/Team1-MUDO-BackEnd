package com.academy.mudogroupware.dataimport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.dataimport.application.command.UpdateImportDraftCommand;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportEnrollmentCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureCandidate;
import com.academy.mudogroupware.dataimport.domain.model.ImportLectureSchedule;
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

class UpdateImportDraftServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 11, 0);

    private final FakeDataImportJobRepository repository = new FakeDataImportJobRepository();
    private final Clock clock = Clock.fixed(NOW.plusMinutes(5).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final UpdateImportDraftService updateService = new UpdateImportDraftService(repository,
            new ImportDraftSanitizer(new ImportDraftValidator()), clock);
    private final GetImportDraftService getService = new GetImportDraftService(repository);

    @Test
    void updateDraftRejectsOtherUsersJob() {
        DataImportJob saved = repository.save(DataImportJob.create(10L, List.of("students.csv"),
                ImportDraft.empty(), NOW));

        assertThatThrownBy(() -> updateService.updateDraft(
                new UpdateImportDraftCommand(11L, saved.getId(), ImportDraft.empty())))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.IMPORT_ACCESS_DENIED);
    }

    @Test
    void updateDraftRejectsConfirmedJob() {
        DataImportJob job = DataImportJob.create(10L, List.of("students.csv"), ImportDraft.empty(), NOW);
        job.confirm(new ImportResult(0, 0, 0, 0, 0), NOW);
        DataImportJob saved = repository.save(job);

        assertThatThrownBy(() -> updateService.updateDraft(
                new UpdateImportDraftCommand(10L, saved.getId(), ImportDraft.empty())))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.IMPORT_ALREADY_CONFIRMED);
    }

    @Test
    void updateDraftRecalculatesStudentStatusInsteadOfTrustingClientStatus() {
        DataImportJob saved = repository.save(DataImportJob.create(10L, List.of("students.csv"),
                ImportDraft.empty(), NOW));
        ImportStudentCandidate clientReadyButInvalid = new ImportStudentCandidate("S1", true,
                ImportRowStatus.READY, "Kim", null, "Mudo High", "010-1111-2222",
                null, null, List.of());

        updateService.updateDraft(new UpdateImportDraftCommand(10L, saved.getId(),
                new ImportDraft(List.of(clientReadyButInvalid), List.of(), List.of())));

        ImportStudentCandidate stored = getService.getDraft(10L, saved.getId()).students().get(0);
        assertThat(stored.status()).isEqualTo(ImportRowStatus.ERROR);
        assertThat(stored.selected()).isFalse();
    }

    @Test
    void updateDraftRelinksEnrollmentToReadyEditedRows() {
        DataImportJob saved = repository.save(DataImportJob.create(10L, List.of("import.csv"),
                ImportDraft.empty(), NOW));
        ImportStudentCandidate student = new ImportStudentCandidate("S1", true, ImportRowStatus.READY,
                "Kim", StudentGrade.HIGH_1, "Mudo High", "010-1111-2222", null, null, List.of());
        ImportLectureCandidate lecture = new ImportLectureCandidate("L1", true, ImportRowStatus.READY,
                "Math", Grade.HIGH_1, "2026 Summer", "Math", 30L, "Teacher", "A101",
                FeeType.PER_MONTH, 300000,
                List.of(new ImportLectureSchedule(DayOfWeek.MONDAY, LocalTime.of(19, 0),
                        LocalTime.of(21, 0))),
                List.of());
        ImportEnrollmentCandidate enrollmentWithoutRowIds = new ImportEnrollmentCandidate("E1", true,
                ImportRowStatus.READY, null, null, "Kim", "010-1111-2222", "Math", "Teacher",
                List.of());

        updateService.updateDraft(new UpdateImportDraftCommand(10L, saved.getId(),
                new ImportDraft(List.of(student), List.of(lecture), List.of(enrollmentWithoutRowIds))));

        ImportEnrollmentCandidate stored = getService.getDraft(10L, saved.getId()).enrollments().get(0);
        assertThat(stored.status()).isEqualTo(ImportRowStatus.READY);
        assertThat(stored.selected()).isTrue();
        assertThat(stored.studentRowId()).isEqualTo("S1");
        assertThat(stored.lectureRowId()).isEqualTo("L1");
    }

    @Test
    void getsDraftByCreatorScope() {
        DataImportJob saved = repository.save(DataImportJob.create(10L, List.of("students.csv"),
                ImportDraft.empty(), NOW));

        ImportDraft draft = getService.getDraft(10L, saved.getId());

        assertThat(draft).isEqualTo(ImportDraft.empty());
    }

    private static final class FakeDataImportJobRepository implements DataImportJobRepository {
        private final List<DataImportJob> jobs = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public DataImportJob save(DataImportJob job) {
            DataImportJob saved = DataImportJob.restore(
                    job.getId() != null ? job.getId() : sequence++,
                    job.getCreatedBy(),
                    job.getStatus(),
                    job.getSourceFileNames(),
                    job.getDraft(),
                    job.getResult(),
                    job.getCreatedAt(),
                    job.getUpdatedAt());
            jobs.removeIf(existing -> existing.getId().equals(saved.getId()));
            jobs.add(saved);
            return saved;
        }

        @Override
        public Optional<DataImportJob> findById(Long id) {
            return jobs.stream().filter(job -> job.getId().equals(id)).findFirst();
        }
    }
}
