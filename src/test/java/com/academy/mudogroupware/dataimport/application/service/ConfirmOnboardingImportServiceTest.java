package com.academy.mudogroupware.dataimport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;
import com.academy.mudogroupware.lecture.application.usecase.CreateLectureUseCase;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.student.application.command.CreateStudentCommand;
import com.academy.mudogroupware.student.application.command.EnrollStudentCommand;
import com.academy.mudogroupware.student.application.usecase.CreateStudentUseCase;
import com.academy.mudogroupware.student.application.usecase.EnrollStudentUseCase;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

class ConfirmOnboardingImportServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 11, 0);

    private final FakeDataImportJobRepository repository = new FakeDataImportJobRepository();
    private final CreateStudentUseCase createStudentUseCase = mock(CreateStudentUseCase.class);
    private final CreateLectureUseCase createLectureUseCase = mock(CreateLectureUseCase.class);
    private final EnrollStudentUseCase enrollStudentUseCase = mock(EnrollStudentUseCase.class);
    private final Clock clock = Clock.fixed(NOW.plusMinutes(10).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final ConfirmOnboardingImportService service = new ConfirmOnboardingImportService(
            repository, createStudentUseCase, createLectureUseCase, enrollStudentUseCase,
            new ImportDraftSanitizer(new ImportDraftValidator()), clock);

    @Test
    void confirmCreatesSelectedReadyRowsOnly() {
        DataImportJob job = repository.save(DataImportJob.create(10L, List.of("import.csv"),
                draftWithReadyStudentLectureEnrollment(), NOW));
        when(createStudentUseCase.createStudent(any())).thenReturn(100L);
        when(createLectureUseCase.createLecture(any())).thenReturn(200L);
        when(enrollStudentUseCase.enroll(any())).thenReturn(300L);

        ImportResult result = service.confirm(job.getId(), 10L);

        assertThat(result.createdStudents()).isEqualTo(1);
        assertThat(result.createdLectures()).isEqualTo(1);
        assertThat(result.createdEnrollments()).isEqualTo(1);
        assertThat(repository.findById(job.getId()).orElseThrow().getResult()).isEqualTo(result);
        verify(createStudentUseCase).createStudent(any(CreateStudentCommand.class));
        verify(createLectureUseCase).createLecture(any(CreateLectureCommand.class));
        verify(enrollStudentUseCase).enroll(new EnrollStudentCommand(100L, 200L));
    }

    @Test
    void confirmRejectsOtherUsersJob() {
        DataImportJob job = repository.save(DataImportJob.create(10L, List.of("import.csv"),
                draftWithReadyStudentLectureEnrollment(), NOW));

        assertThatThrownBy(() -> service.confirm(job.getId(), 11L))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.IMPORT_ACCESS_DENIED);
    }

    @Test
    void confirmRejectsSelectedRowsThatAreNotReady() {
        DataImportJob job = repository.save(DataImportJob.create(10L, List.of("import.csv"),
                draftWithSelectedNeedsReviewLecture(), NOW));

        assertThatThrownBy(() -> service.confirm(job.getId(), 10L))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.SELECTED_ROW_NOT_READY);
    }

    @Test
    void confirmRevalidatesDraftInsteadOfTrustingStoredReadyStatus() {
        ImportStudentCandidate invalidButReady = new ImportStudentCandidate("S1", true, ImportRowStatus.READY,
                "Kim", null, "Mudo High", "010-1111-2222", null, null, List.of());
        DataImportJob job = repository.save(DataImportJob.create(10L, List.of("students.csv"),
                new ImportDraft(List.of(invalidButReady), List.of(), List.of()), NOW));

        assertThatThrownBy(() -> service.confirm(job.getId(), 10L))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.SELECTED_ROW_NOT_READY);
    }

    private ImportDraft draftWithReadyStudentLectureEnrollment() {
        ImportStudentCandidate student = new ImportStudentCandidate("S1", true, ImportRowStatus.READY,
                "김민수", StudentGrade.HIGH_1, "무도고", "010-1111-2222", null, null, List.of());
        ImportLectureCandidate lecture = new ImportLectureCandidate("L1", true, ImportRowStatus.READY,
                "고1 수학", Grade.HIGH_1, "2026 여름", "수학", 30L, null, "101호",
                FeeType.PER_SESSION, 50000,
                List.of(new ImportLectureSchedule(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0))),
                List.of());
        ImportEnrollmentCandidate enrollment = new ImportEnrollmentCandidate("E1", true, ImportRowStatus.READY,
                "S1", "L1", "김민수", "010-1111-2222", "고1 수학", null, List.of());
        return new ImportDraft(List.of(student), List.of(lecture), List.of(enrollment));
    }

    private ImportDraft draftWithSelectedNeedsReviewLecture() {
        ImportLectureCandidate lecture = new ImportLectureCandidate("L1", true, ImportRowStatus.NEEDS_REVIEW,
                "고1 수학", Grade.HIGH_1, "2026 여름", "수학", null, "박선생", "101호",
                null, null,
                List.of(new ImportLectureSchedule(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0))),
                List.of("강사 ID 확인이 필요합니다."));
        return new ImportDraft(List.of(), List.of(lecture), List.of());
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
