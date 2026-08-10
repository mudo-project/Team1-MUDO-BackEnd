package com.academy.mudogroupware.approval.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
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
        PageResult<ApprovalDocument> result = approvalDocumentRepository.findAllByApproverId(userId, page, size);
        SummaryLookup lookup = buildSummaryLookup(result.content());
        return result.map(document -> toSummaryView(document, userId, lookup));
    }

    @Override
    public PageResult<ApprovalSubmittedSummaryView> getAllApprovals(int page, int size) {
        PageResult<ApprovalDocument> result = approvalDocumentRepository.findAll(page, size);
        SummaryLookup lookup = buildSummaryLookup(result.content());
        return result.map(document -> toSubmittedSummaryView(document, lookup));
    }

    @Override
    public PageResult<ApprovalSummaryView> getMyApprovalHistory(Long userId, int page, int size) {
        PageResult<ApprovalDocument> result = approvalDocumentRepository.findHistoryByApproverId(userId, page, size);
        SummaryLookup lookup = buildSummaryLookup(result.content());
        return result.map(document -> toSummaryView(document, userId, lookup));
    }

    @Override
    public PageResult<ApprovalSubmittedSummaryView> getMySubmittedApprovals(Long userId, int page, int size) {
        PageResult<ApprovalDocument> result = approvalDocumentRepository.findAllByCreatorId(userId, page, size);
        SummaryLookup lookup = buildSummaryLookup(result.content());
        return result.map(document -> toSubmittedSummaryView(document, lookup));
    }

    @Override
    public long getMyPendingCount(Long userId) {
        return approvalDocumentRepository.countPendingByApproverId(userId);
    }

    @Override
    public ApprovalDetailView getApprovalDetail(Long documentId, Long requesterId, boolean canReadAll) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        boolean canReadAsParticipant = approvalDocument.isApprover(requesterId)
                || approvalDocument.getCreatorId().equals(requesterId);
        if (!canReadAsParticipant && !canReadAll) {
            throw new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        List<Long> approverIds = Stream.concat(
                        approvalDocument.getLines().stream().map(ApprovalDocumentLine::getApproverId),
                        Stream.of(approvalDocument.getCreatorId()))
                .distinct()
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
                approvalDocument.getSourceType(),
                approvalDocument.getTemplateId(),
                findTemplateName(approvalDocument.getTemplateId()),
                approvalDocument.getTitle(),
                approvalDocument.getContent().getType(),
                approvalDocument.getContent().getText(),
                attachments,
                approvalDocument.getCreatorId(),
                approverName(approvers, approvalDocument.getCreatorId()),
                approvalDocument.getStatus(),
                approvalDocument.getCreatedAt(),
                lines
        );
    }

    private ApprovalSummaryView toSummaryView(
            ApprovalDocument approvalDocument,
            Long userId,
            SummaryLookup lookup
    ) {
        ApprovalDocumentLine myLine = approvalDocument.getLines().stream()
                .filter(line -> line.getApproverId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED));

        ApprovalDocumentLine currentLine = findCurrentPendingLine(approvalDocument);

        return new ApprovalSummaryView(
                approvalDocument.getId(),
                approvalDocument.getSourceType(),
                approvalDocument.getTitle(),
                templateName(lookup, approvalDocument.getTemplateId()),
                approverName(lookup, approvalDocument.getCreatorId()),
                approvalDocument.getStatus(),
                myLine.getStepOrder(),
                myLine.getStatus(),
                currentLine != null ? currentLine.getStepOrder() : null,
                currentLine != null ? approverName(lookup, currentLine.getApproverId()) : null,
                approvalDocument.getCreatedAt()
        );
    }

    private ApprovalSubmittedSummaryView toSubmittedSummaryView(
            ApprovalDocument approvalDocument,
            SummaryLookup lookup
    ) {
        ApprovalDocumentLine currentLine = findCurrentPendingLine(approvalDocument);

        return new ApprovalSubmittedSummaryView(
                approvalDocument.getId(),
                approvalDocument.getSourceType(),
                approvalDocument.getTitle(),
                templateName(lookup, approvalDocument.getTemplateId()),
                approverName(lookup, approvalDocument.getCreatorId()),
                approvalDocument.getStatus(),
                currentLine != null ? currentLine.getStepOrder() : null,
                currentLine != null ? approverName(lookup, currentLine.getApproverId()) : null,
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
        if (approvalDocument.getStatus() != ApprovalStatus.IN_PROGRESS) {
            return null;
        }
        return approvalDocument.getLines().stream()
                .filter(line -> line.getStatus() == ApprovalLineStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private String approverName(Map<Long, ApproverInfo> approvers, Long userId) {
        ApproverInfo approverInfo = approvers.get(userId);
        return approverInfo != null ? approverInfo.name() : null;
    }

    private SummaryLookup buildSummaryLookup(List<ApprovalDocument> documents) {
        List<Long> templateIds = documents.stream()
                .map(ApprovalDocument::getTemplateId)
                .distinct()
                .toList();
        Map<Long, String> templateNames = approvalTemplateRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(ApprovalTemplate::getId, ApprovalTemplate::getName, (a, b) -> a));

        List<Long> userIds = documents.stream()
                .flatMap(document -> Stream.concat(
                        Stream.of(document.getCreatorId()),
                        document.currentPendingApproverId().stream()
                ))
                .distinct()
                .toList();
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(userIds);

        return new SummaryLookup(templateNames, approvers);
    }

    private String templateName(SummaryLookup lookup, Long templateId) {
        return lookup.templateNames().get(templateId);
    }

    private String approverName(SummaryLookup lookup, Long userId) {
        ApproverInfo approverInfo = lookup.approvers().get(userId);
        return approverInfo != null ? approverInfo.name() : null;
    }

    private String findTemplateName(Long templateId) {
        return approvalTemplateRepository.findById(templateId)
                .map(template -> template.getName())
                .orElse(null);
    }

    private record SummaryLookup(
            Map<Long, String> templateNames,
            Map<Long, ApproverInfo> approvers
    ) {
    }
}
