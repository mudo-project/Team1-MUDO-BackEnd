package com.academy.mudogroupware.dataimport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.dataimport.domain.model.DataImportJob;
import com.academy.mudogroupware.dataimport.domain.model.DataImportStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.dataimport.domain.model.ImportRowStatus;
import com.academy.mudogroupware.dataimport.domain.model.ImportStudentCandidate;
import com.academy.mudogroupware.dataimport.domain.repository.DataImportJobRepository;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(DataImportJobRepositoryImpl.class)
class DataImportJobRepositoryImplDataJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 10, 0);

    @Autowired
    private DataImportJobRepository repository;

    @Test
    void savesAndRestoresDraftJson() {
        ImportDraft draft = new ImportDraft(
                List.of(new ImportStudentCandidate("S1", true, ImportRowStatus.READY,
                        "김민수", StudentGrade.HIGH_1, "무도고", "010-1111-2222", null, null, List.of())),
                List.of(),
                List.of());
        DataImportJob saved = repository.save(DataImportJob.create(10L,
                List.of("students.xlsx"), draft, NOW));

        DataImportJob restored = repository.findById(saved.getId()).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(DataImportStatus.DRAFT);
        assertThat(restored.getDraft().students()).hasSize(1);
        assertThat(restored.getDraft().students().get(0).name()).isEqualTo("김민수");
        assertThat(restored.getSourceFileNames()).containsExactly("students.xlsx");
    }

    @Test
    void savesAndRestoresResultJson() {
        DataImportJob job = DataImportJob.create(10L, List.of("students.xlsx"), ImportDraft.empty(), NOW);
        job.confirm(new ImportResult(1, 2, 3, 4, 5), NOW.plusMinutes(1));

        DataImportJob saved = repository.save(job);
        DataImportJob restored = repository.findById(saved.getId()).orElseThrow();

        assertThat(restored.getStatus()).isEqualTo(DataImportStatus.CONFIRMED);
        assertThat(restored.getResult()).isEqualTo(new ImportResult(1, 2, 3, 4, 5));
        assertThat(restored.getUpdatedAt()).isEqualTo(NOW.plusMinutes(1));
    }
}
