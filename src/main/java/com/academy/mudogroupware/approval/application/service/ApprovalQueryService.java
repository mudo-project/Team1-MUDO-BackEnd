package com.academy.mudogroupware.approval.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalLineView;
import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalQueryUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalQueryService implements ApprovalQueryUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApproverDirectoryPort approverDirectoryPort;

    @Override
    public List<ApprovalSummaryView> getMyApprovals(Long userId) {
        return approvalDocumentRepository.findAllByApproverId(userId).stream()
                .map(document -> toSummaryView(document, userId))
                .toList();
    }

    @Override
    public List<ApprovalSubmittedSummaryView> getMySubmittedApprovals(Long userId) {
        return approvalDocumentRepository.findAllByCreatorId(userId).stream()
                .map(this::toSubmittedSummaryView)
                .toList();
    }

    @Override
    public ApprovalDetailView getApprovalDetail(Long documentId, Long requesterId) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("결재 문서를 찾을 수 없습니다."));

        if (!approvalDocument.isApprover(requesterId) && !approvalDocument.getCreatorId().equals(requesterId)) {
            throw new ForbiddenException("해당 결재를 조회할 권한이 없습니다.");
        }

        List<Long> approverIds = approvalDocument.getLines().stream()
                .map(ApprovalDocumentLine::getApproverId)
                .toList();
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(approverIds);

        List<ApprovalLineView> lines = approvalDocument.getLines().stream()
                .map(line -> toLineView(line, approvers))
                .toList();

        return new ApprovalDetailView(
                approvalDocument.getId(),
                approvalDocument.getTemplateId(),
                approvalDocument.getTitle(),
                approvalDocument.getContent().getType(),
                approvalDocument.getContent().getText(),
                approvalDocument.getContent().getFileUrl(),
                approvalDocument.getCreatorId(),
                approvalDocument.getStatus(),
                approvalDocument.getCreatedAt(),
                lines
        );
    }

    private ApprovalSummaryView toSummaryView(ApprovalDocument approvalDocument, Long userId) {
        ApprovalDocumentLine myLine = approvalDocument.getLines().stream()
                .filter(line -> line.getApproverId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("해당 결재를 조회할 권한이 없습니다."));

        return new ApprovalSummaryView(
                approvalDocument.getId(),
                approvalDocument.getTitle(),
                approvalDocument.getStatus(),
                myLine.getStepOrder(),
                myLine.getStatus(),
                approvalDocument.getCreatedAt()
        );
    }

    private ApprovalSubmittedSummaryView toSubmittedSummaryView(ApprovalDocument approvalDocument) {
        return new ApprovalSubmittedSummaryView(
                approvalDocument.getId(),
                approvalDocument.getTitle(),
                approvalDocument.getStatus(),
                approvalDocument.getCreatedAt()
        );
    }

    private ApprovalLineView toLineView(ApprovalDocumentLine line, Map<Long, ApproverInfo> approvers) {
        ApproverInfo approverInfo = approvers.get(line.getApproverId());
        String approverName = approverInfo != null ? approverInfo.name() : null;

        return new ApprovalLineView(
                line.getId(),
                line.getStepOrder(),
                line.getApproverId(),
                approverName,
                line.getStatus(),
                line.getComment(),
                line.getDecidedAt()
        );
    }
}
