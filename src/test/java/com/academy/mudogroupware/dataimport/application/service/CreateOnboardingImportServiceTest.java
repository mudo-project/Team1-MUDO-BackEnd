package com.academy.mudogroupware.dataimport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.dataimport.application.command.CreateOnboardingImportCommand;
import com.academy.mudogroupware.dataimport.application.port.ImportFile;
import com.academy.mudogroupware.dataimport.application.port.ImportFileParserPort;
import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;
import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;

class CreateOnboardingImportServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 11, 0);

    private final FakeDataImportJobRepository repository = new FakeDataImportJobRepository();
    private final ImportFileParserPort parser = file -> new ParsedImportSheet(file.role(), file.fileName(),
            List.of(new ParsedImportRow(2, Map.of("이름", "김민수", "학년", "고1"))));
    private final ImportDraftBuilder draftBuilder = new ImportDraftBuilder(new ImportValueNormalizer(),
            new ImportDraftValidator());
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final CreateOnboardingImportService service = new CreateOnboardingImportService(
            repository, parser, draftBuilder, clock);

    @Test
    void createFailsWhenNoFilesUploaded() {
        CreateOnboardingImportCommand command = new CreateOnboardingImportCommand(1L, 10L, List.of());

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.FILE_REQUIRED);
    }

    @Test
    void createsImportJobFromUploadedFile() {
        Long importId = service.create(new CreateOnboardingImportCommand(1L, 10L,
                List.of(new ImportFile(ImportFileRole.STUDENT, "students.csv", new byte[] {1, 2, 3}))));

        DataImportJob saved = repository.findById(importId).orElseThrow();
        assertThat(saved.getAcademyId()).isEqualTo(1L);
        assertThat(saved.getCreatedBy()).isEqualTo(10L);
        assertThat(saved.getSourceFileNames()).containsExactly("students.csv");
        assertThat(saved.getDraft().students()).hasSize(1);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
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
