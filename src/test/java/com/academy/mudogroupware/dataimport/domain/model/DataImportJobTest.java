package com.academy.mudogroupware.dataimport.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;

class DataImportJobTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 10, 0);

    @Test
    void createsDraftJobWithAcademyScope() {
        ImportDraft draft = ImportDraft.empty();
        DataImportJob job = DataImportJob.create(1L, 10L, List.of("students.xlsx"), draft, NOW);

        assertThat(job.getAcademyId()).isEqualTo(1L);
        assertThat(job.getCreatedBy()).isEqualTo(10L);
        assertThat(job.getStatus()).isEqualTo(DataImportStatus.DRAFT);
        assertThat(job.getSourceFileNames()).containsExactly("students.xlsx");
        assertThat(job.getDraft()).isEqualTo(draft);
    }

    @Test
    void cannotUpdateDraftAfterConfirmed() {
        DataImportJob job = DataImportJob.create(1L, 10L, List.of("students.xlsx"), ImportDraft.empty(), NOW);
        job.confirm(new ImportResult(1, 0, 0, 0, 0), NOW);

        assertThatThrownBy(() -> job.updateDraft(ImportDraft.empty(), NOW))
                .isInstanceOf(DataImportException.class)
                .extracting("errorCode")
                .isEqualTo(DataImportErrorCode.IMPORT_ALREADY_CONFIRMED);
    }
}
