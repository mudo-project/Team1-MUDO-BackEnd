package com.academy.mudogroupware.approval.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;
import com.academy.mudogroupware.approval.domain.model.ApprovalRetentionPolicy;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(ApprovalDocumentRepositoryImpl.class)
class ApprovalDocumentRepositoryImplDataJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 10, 0);

    @Autowired
    private ApprovalDocumentRepositoryImpl approvalDocumentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updatingLinesReusesStepOrdersWithoutViolatingUniqueConstraint() {
        jdbcTemplate.execute("""
                create unique index uk_approval_step_document_step
                on approval_step (approval_document_id, step_order)
                """);
        ApprovalDocument created = approvalDocumentRepository.save(
                ApprovalDocument.create(1L, "vacation", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                        100L, List.of(10L, 20L), List.of(), NOW));
        entityManager.flush();
        entityManager.clear();

        ApprovalDocument toUpdate = approvalDocumentRepository.findById(created.getId()).orElseThrow();
        toUpdate.updateLines(List.of(30L, 40L));

        approvalDocumentRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        ApprovalDocument reloaded = approvalDocumentRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getLines()).extracting(line -> line.getApproverId())
                .containsExactly(30L, 40L);
    }

    @Test
    void saveAndFindPreservesRetentionPolicy() {
        ApprovalDocument created = approvalDocumentRepository.save(
                ApprovalDocument.create(1L, ApprovalDocumentSourceType.CORPORATE_CARD_EXPENSE,
                        "card expense", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                        100L, List.of(10L), List.of(), NOW));
        entityManager.flush();
        entityManager.clear();

        ApprovalDocument reloaded = approvalDocumentRepository.findById(created.getId()).orElseThrow();

        assertThat(reloaded.getRetentionPolicy()).isEqualTo(ApprovalRetentionPolicy.TAX_EVIDENCE);
        assertThat(reloaded.getRetentionUntil()).isEqualTo(NOW.plusYears(5));
        assertThat(reloaded.isLegalHold()).isFalse();
        assertThat(reloaded.getArchivedAt()).isNull();
    }
}
