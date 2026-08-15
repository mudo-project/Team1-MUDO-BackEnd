package com.academy.mudogroupware.approval.infrastructure.corporatecard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplateLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.infrastructure.persistence.ApprovalDocumentJpaRepository;

class CorporateCardApprovalSubmissionAdapterTest {

    private static final Long TEMPLATE_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 10, 0);

    private final ApprovalTemplateRepository templateRepository = mock(ApprovalTemplateRepository.class);
    private final ApprovalDocumentRepository documentRepository = mock(ApprovalDocumentRepository.class);
    private final CorporateCardApprovalSubmissionAdapter adapter = new CorporateCardApprovalSubmissionAdapter(
            mock(CreateApprovalDocumentUseCase.class),
            documentRepository,
            templateRepository,
            mock(ApprovalDocumentJpaRepository.class));

    @Test
    void savesFirstApprovalLinesAfterLoadingTemplateWithWriteLock() {
        ApprovalTemplate template = ApprovalTemplate.restore(
                TEMPLATE_ID, "법인카드 정산", 1L, null, NOW, NOW);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID)).thenReturn(Optional.of(template));

        adapter.saveDefaultApproverIdsIfEmpty(TEMPLATE_ID, List.of(10L, 20L));

        assertThat(template.approverIdsInOrder()).containsExactly(10L, 20L);
        verify(templateRepository).save(template);
    }

    @Test
    void keepsExistingDefaultApprovalLines() {
        ApprovalTemplate template = ApprovalTemplate.restore(
                TEMPLATE_ID, "법인카드 정산", 1L, List.of(
                        ApprovalTemplateLine.restore(1L, 1, 10L, null)), NOW, NOW);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID)).thenReturn(Optional.of(template));

        adapter.saveDefaultApproverIdsIfEmpty(TEMPLATE_ID, List.of(20L));

        assertThat(template.approverIdsInOrder()).containsExactly(10L);
        verify(templateRepository, never()).save(template);
    }

    @Test
    void returnsDocumentApprovalLinesInStepOrder() {
        ApprovalDocument document = mock(ApprovalDocument.class);
        ApprovalDocumentLine second = mock(ApprovalDocumentLine.class);
        ApprovalDocumentLine first = mock(ApprovalDocumentLine.class);
        when(second.getApproverId()).thenReturn(20L);
        when(second.getStepOrder()).thenReturn(2);
        when(first.getApproverId()).thenReturn(10L);
        when(first.getStepOrder()).thenReturn(1);
        when(document.getLines()).thenReturn(List.of(second, first));
        when(documentRepository.findById(99L)).thenReturn(Optional.of(document));

        var result = adapter.findApprovalLines(99L);

        assertThat(result).containsExactly(
                new com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort.ApprovalLineInfo(
                        10L, 1),
                new com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort.ApprovalLineInfo(
                        20L, 2));
    }
}
