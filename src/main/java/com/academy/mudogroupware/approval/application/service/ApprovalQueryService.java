package com.academy.mudogroupware.approval.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentView;
import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalLineView;
import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalQueryUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalQueryService implements ApprovalQueryUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApproverDirectoryPort approverDirectoryPort;

    @Override
    public PageResult<ApprovalSummaryView> getMyApprovals(Long userId, int page, int size) {
        return approvalDocumentRepository.findAllByApproverId(userId, page, size)
                .map(document -> toSummaryView(document, userId));
    }

    @Override
    public PageResult<ApprovalSubmittedSummaryView> getMySubmittedApprovals(Long userId, int page, int size) {
        return approvalDocumentRepository.findAllByCreatorId(userId, page, size)
                .map(this::toSubmittedSummaryView);
    }

    @Override
    public long getMyPendingCount(Long userId) {
        return approvalDocumentRepository.findAllByApproverId(userId).stream()
                .filter(document -> document.getStatus() == ApprovalStatus.IN_PROGRESS)
                .filter(document -> document.getLines().stream()
                        .anyMatch(line -> line.getApproverId().equals(userId)
                                && line.getStatus() == ApprovalLineStatus.PENDING))
                .count();
    }

    @Override
    public ApprovalDetailView getApprovalDetail(Long documentId, Long requesterId) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!approvalDocument.isApprover(requesterId) && !approvalDocument.getCreatorId().equals(requesterId)) {
            throw new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        List<Long> approverIds = approvalDocument.getLines().stream()
                .map(ApprovalDocumentLine::getApproverId)
                .toList();
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(approverIds);

        List<ApprovalLineView> lines = approvalDocument.getLines().stream()
                .map(line -> toLineView(line, approvers))
                .toList();
        List<ApprovalAttachmentView> attachments = approvalDocument.getAttachments().stream()
                .map(this::toAttachmentView)
                .toList();

        return new ApprovalDetailView(
                approvalDocument.getId(),
                approvalDocument.getTemplateId(),
                findTemplateName(approvalDocument.getTemplateId()),
                approvalDocument.getTitle(),
                approvalDocument.getContent().getType(),
                approvalDocument.getContent().getText(),
                attachments,
                approvalDocument.getCreatorId(),
                findApproverName(approvalDocument.getCreatorId()),
                approvalDocument.getStatus(),
                approvalDocument.getCreatedAt(),
                lines
        );
    }

    private ApprovalSummaryView toSummaryView(ApprovalDocument approvalDocument, Long userId) {
        ApprovalDocumentLine myLine = approvalDocument.getLines().stream()
                .filter(line -> line.getApproverId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED));

        ApprovalDocumentLine currentLine = findCurrentPendingLine(approvalDocument);

        return new ApprovalSummaryView(
                approvalDocument.getId(),
                approvalDocument.getTitle(),
                findTemplateName(approvalDocument.getTemplateId()),
                findApproverName(approvalDocument.getCreatorId()),
                approvalDocument.getStatus(),
                myLine.getStepOrder(),
                myLine.getStatus(),
                currentLine != null ? currentLine.getStepOrder() : null,
                currentLine != null ? findApproverName(currentLine.getApproverId()) : null,
                approvalDocument.getCreatedAt()
        );
    }

    private ApprovalSubmittedSummaryView toSubmittedSummaryView(ApprovalDocument approvalDocument) {
        ApprovalDocumentLine currentLine = findCurrentPendingLine(approvalDocument);

        return new ApprovalSubmittedSummaryView(
                approvalDocument.getId(),
                approvalDocument.getTitle(),
                findTemplateName(approvalDocument.getTemplateId()),
                findApproverName(approvalDocument.getCreatorId()),
                approvalDocument.getStatus(),
                currentLine != null ? currentLine.getStepOrder() : null,
                currentLine != null ? findApproverName(currentLine.getApproverId()) : null,
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

    private ApprovalAttachmentView toAttachmentView(ApprovalAttachment attachment) {
        return new ApprovalAttachmentView(
                attachment.getFileId(),
                attachment.getAiSummary(),
                attachment.getSummaryStatus(),
                attachment.getSummarizedAt()
        );
    }

    private ApprovalDocumentLine findCurrentPendingLine(ApprovalDocument approvalDocument) {
        return approvalDocument.getLines().stream()
                .filter(line -> line.getStatus() == ApprovalLineStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private String findTemplateName(Long templateId) {
        return approvalTemplateRepository.findById(templateId)
                .map(template -> template.getName())
                .orElse(null);
    }

    private String findApproverName(Long userId) {
        ApproverInfo approverInfo = approverDirectoryPort.getApprovers(List.of(userId)).get(userId);
        return approverInfo != null ? approverInfo.name() : null;
    }
}
