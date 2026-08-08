package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDecision;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplateLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

class ApprovalQueryServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final ApprovalTemplateRepository approvalTemplateRepository = mock(ApprovalTemplateRepository.class);
    private final ApproverDirectoryPort approverDirectoryPort = mock(ApproverDirectoryPort.class);

    private ApprovalQueryService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalQueryService(approvalDocumentRepository, approvalTemplateRepository, approverDirectoryPort);
        when(approvalTemplateRepository.findById(1L)).thenReturn(Optional.empty());
        when(approvalTemplateRepository.findAllById(anyList())).thenReturn(List.of());
        when(approverDirectoryPort.getApprovers(anyList())).thenAnswer(invocation -> {
            List<Long> userIds = invocation.getArgument(0);
            Map<Long, ApproverInfo> approvers = new HashMap<>();
            for (Long userId : userIds) {
                approvers.put(userId, new ApproverInfo(userId, "user-" + userId, 1L));
            }
            return approvers;
        });
    }

    @Test
    void getAllApprovalsReturnsAcademyScopedDocuments() {
        ApprovalDocument document = document(1L, 1L);
        when(approvalDocumentRepository.findAllByAcademyId(1L, 0, 20))
                .thenReturn(PageResult.of(List.of(document), 0, 20, false));

        PageResult<ApprovalSubmittedSummaryView> result = service.getAllApprovals(1L, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(1L);
        assertThat(result.content().get(0).creatorName()).isEqualTo("user-7");
    }

    @Test
    void getAllApprovalsResolvesTemplatesAndApproversInBatch() {
        ApprovalDocument first = document(1L, 1L, 1L, 7L, 12L);
        ApprovalDocument second = document(2L, 1L, 2L, 8L, 13L);
        when(approvalDocumentRepository.findAllByAcademyId(1L, 0, 20))
                .thenReturn(PageResult.of(List.of(first, second), 0, 20, false));
        when(approvalTemplateRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(template(1L, "Vacation"), template(2L, "Expense")));

        PageResult<ApprovalSubmittedSummaryView> result = service.getAllApprovals(1L, 0, 20);

        assertThat(result.content()).extracting(ApprovalSubmittedSummaryView::templateName)
                .containsExactly("Vacation", "Expense");
        assertThat(result.content()).extracting(ApprovalSubmittedSummaryView::creatorName)
                .containsExactly("user-7", "user-8");
        assertThat(result.content()).extracting(ApprovalSubmittedSummaryView::currentApproverName)
                .containsExactly("user-12", "user-13");
        verify(approvalTemplateRepository).findAllById(List.of(1L, 2L));
        verify(approvalTemplateRepository, never()).findById(1L);
        verify(approvalTemplateRepository, never()).findById(2L);
    }

    @Test
    void getMyApprovalHistoryReturnsOnlyDocumentsWithMyDecidedLine() {
        ApprovalDocument document = document(1L, 1L);
        document.decide(12L, ApprovalDecision.APPROVE, null, CREATED_AT.plusHours(1));
        when(approvalDocumentRepository.findHistoryByApproverId(12L, 0, 20))
                .thenReturn(PageResult.of(List.of(document), 0, 20, false));

        PageResult<ApprovalSummaryView> result = service.getMyApprovalHistory(12L, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).myLineStatus()).isEqualTo(ApprovalLineStatus.APPROVED);
    }

    @Test
    void getMyPendingCountUsesRepositoryCount() {
        when(approvalDocumentRepository.countPendingByApproverId(12L)).thenReturn(5L);

        long count = service.getMyPendingCount(12L);

        assertThat(count).isEqualTo(5L);
        verify(approvalDocumentRepository).countPendingByApproverId(12L);
        verify(approvalDocumentRepository, never()).findAllByApproverId(12L);
    }

    @Test
    void getApprovalDetailAllowsReadAllRequesterFromSameAcademy() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document(1L, 1L)));

        ApprovalDetailView result = service.getApprovalDetail(1L, 99L, 1L, true);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getApprovalDetailRejectsReadAllRequesterFromAnotherAcademy() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document(1L, 1L)));

        assertThatThrownBy(() -> service.getApprovalDetail(1L, 99L, 2L, true))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
    }

    private ApprovalDocument document(Long documentId, Long academyId) {
        return document(documentId, academyId, 1L, 7L, 12L);
    }

    private ApprovalDocument document(Long documentId, Long academyId, Long templateId, Long creatorId,
                                      Long approverId) {
        return ApprovalDocument.restore(
                documentId,
                academyId,
                templateId,
                "Vacation",
                ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                creatorId,
                ApprovalDocument.create(academyId, templateId, "Vacation",
                        ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                        creatorId, List.of(approverId), List.of(), CREATED_AT).getLines(),
                List.of(),
                ApprovalStatus.IN_PROGRESS,
                CREATED_AT,
                null);
    }

    private ApprovalTemplate template(Long templateId, String name) {
        return ApprovalTemplate.restore(templateId, 1L, name, 7L,
                List.of(ApprovalTemplateLine.create(1, 12L)), CREATED_AT, CREATED_AT);
    }
}
