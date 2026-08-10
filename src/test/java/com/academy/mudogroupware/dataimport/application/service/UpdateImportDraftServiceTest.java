package com.academy.mudogroupware.dataimport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDateTime;
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
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;

class UpdateImportDraftServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 11, 0);

    private final FakeDataImportJobRepository repository = new FakeDataImportJobRepository();
    private final Clock clock = Clock.fixed(NOW.plusMinutes(5).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final UpdateImportDraftService updateService = new UpdateImportDraftService(repository, clock);
    private final GetImportDraftService getService = new GetImportDraftService(repository);

    @Test
    void updateDraftRejectsOtherAcademyJob() {
        DataImportJob saved = repository.save(DataImportJob.create(1L, 10L, List.of("students.csv"),
                ImportDraft.empty(), NOW));

        assertThatThrownBy(() -> updateService.updateDraft(
                new UpdateImportDraftCommand(2L, saved.getId(), ImportDraft.empty())))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.IMPORT_ACCESS_DENIED);
    }

    @Test
    void updateDraftRejectsConfirmedJob() {
        DataImportJob job = DataImportJob.create(1L, 10L, List.of("students.csv"), ImportDraft.empty(), NOW);
        job.confirm(new ImportResult(0, 0, 0, 0, 0), NOW);
        DataImportJob saved = repository.save(job);

        assertThatThrownBy(() -> updateService.updateDraft(
                new UpdateImportDraftCommand(1L, saved.getId(), ImportDraft.empty())))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.IMPORT_ALREADY_CONFIRMED);
    }

    @Test
    void getsDraftByAcademyScope() {
        DataImportJob saved = repository.save(DataImportJob.create(1L, 10L, List.of("students.csv"),
                ImportDraft.empty(), NOW));

        ImportDraft draft = getService.getDraft(1L, saved.getId());

        assertThat(draft).isEqualTo(ImportDraft.empty());
    }

    private static final class FakeDataImportJobRepository implements DataImportJobRepository {
        private final List<DataImportJob> jobs = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public DataImportJob save(DataImportJob job) {
            DataImportJob saved = DataImportJob.restore(
                    job.getId() != null ? job.getId() : sequence++,
                    job.getAcademyId(),
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
