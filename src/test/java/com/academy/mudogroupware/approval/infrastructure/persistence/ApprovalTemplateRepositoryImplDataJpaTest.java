package com.academy.mudogroupware.approval.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, ApprovalTemplateRepositoryImpl.class})
class ApprovalTemplateRepositoryImplDataJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    @Autowired
    private ApprovalTemplateRepositoryImpl approvalTemplateRepository;

    @Test
    void updatingLinesReusesTheSameStepOrdersWithoutViolatingUniqueConstraint() {
        // buildLines()는 항상 step_order 1부터 다시 채번하므로, 결재선 구성만 바뀌는
        // 일반적인 수정에서도 항상 기존 step_order와 충돌한다(clearLines 후 재삽입 시
        // orphanRemoval DELETE보다 INSERT가 먼저 나가는 Hibernate flush 순서 문제).
        ApprovalTemplate created = approvalTemplateRepository.save(
                ApprovalTemplate.create("휴가 신청서", 100L, List.of(10L, 20L), NOW));

        ApprovalTemplate toUpdate = approvalTemplateRepository.findById(created.getId()).orElseThrow();
        toUpdate.update("휴가 신청서(수정)", List.of(30L, 40L), NOW.plusMinutes(1));

        approvalTemplateRepository.save(toUpdate);

        ApprovalTemplate reloaded = approvalTemplateRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("휴가 신청서(수정)");
        assertThat(reloaded.approverIdsInOrder()).containsExactly(30L, 40L);
    }

    @Test
    void updatingWithFewerApproversRemovesOrphanedLines() {
        ApprovalTemplate created = approvalTemplateRepository.save(
                ApprovalTemplate.create("휴가 신청서", 100L, List.of(10L, 20L, 30L), NOW));

        ApprovalTemplate toUpdate = approvalTemplateRepository.findById(created.getId()).orElseThrow();
        toUpdate.update("휴가 신청서", List.of(40L), NOW.plusMinutes(1));
        approvalTemplateRepository.save(toUpdate);

        ApprovalTemplate reloaded = approvalTemplateRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.approverIdsInOrder()).containsExactly(40L);
    }
}
