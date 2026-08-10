package com.academy.mudogroupware.corporatecard.infrastructure.approval;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.infrastructure.persistence.ApprovalDocumentJpaRepository;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalSubmissionPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalSubmissionAdapter implements ApprovalSubmissionPort {
    private final CreateApprovalDocumentUseCase createApprovalDocumentUseCase;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalDocumentJpaRepository approvalDocumentJpaRepository;

    @Override
    public Long submit(Long templateId, Long creatorId, String title, String content, List<Long> approverIds) {
        return createApprovalDocumentUseCase.createDocument(new CreateApprovalDocumentCommand(
                templateId, title, ApprovalContentType.TEXT, content, null, creatorId, approverIds, null, null,
                ApprovalDocumentSourceType.CORPORATE_CARD_EXPENSE));
    }

    @Override
    public ApprovalStatusView findStatus(Long documentId) {
        return approvalDocumentRepository.findById(documentId)
                .map(document -> new ApprovalStatusView(document.getStatus().name(), document.getStatus().name()))
                .orElse(null);
    }

    @Override
    public Map<Long, ApprovalStatusView> findStatuses(Set<Long> documentIds) {
        return approvalDocumentJpaRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> new ApprovalStatusView(d.getStatus().name(), d.getStatus().name())));
    }
}
