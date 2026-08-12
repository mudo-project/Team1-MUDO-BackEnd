package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.model.ApprovalLineStatus;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ApprovalDocumentRepositoryImpl implements ApprovalDocumentRepository {

    private final ApprovalDocumentJpaRepository approvalDocumentJpaRepository;

    @Override
    public ApprovalDocument save(ApprovalDocument approvalDocument) {
        ApprovalDocumentEntity entity = needsLineReplacement(approvalDocument)
                ? updateExistingLines(approvalDocument)
                : toEntity(approvalDocument);
        return toDomain(approvalDocumentJpaRepository.save(entity));
    }

    @Override
    public Optional<ApprovalDocument> findById(Long id) {
        return approvalDocumentJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ApprovalDocument> findAllByApproverId(Long approverId) {
        return approvalDocumentJpaRepository.findAllByApproverId(approverId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countPendingByApproverId(Long approverId) {
        return approvalDocumentJpaRepository.countPendingByApproverId(
                approverId, ApprovalStatus.IN_PROGRESS, ApprovalLineStatus.PENDING);
    }

    @Override
    public PageResult<ApprovalDocument> findAllByApproverId(Long approverId, int page, int size) {
        Slice<ApprovalDocumentEntity> slice = approvalDocumentJpaRepository.findAllByApproverId(
                approverId, PageRequest.of(page, size, latestFirstSort()));
        return toPageResult(slice);
    }

    @Override
    public PageResult<ApprovalDocument> findAllByCreatorId(Long creatorId, int page, int size) {
        Slice<ApprovalDocumentEntity> slice = approvalDocumentJpaRepository.findAllByCreatorId(
                creatorId, PageRequest.of(page, size, latestFirstSort()));
        return toPageResult(slice);
    }

    @Override
    public PageResult<ApprovalDocument> findAll(int page, int size) {
        Slice<ApprovalDocumentEntity> slice = approvalDocumentJpaRepository.findAllBy(
                PageRequest.of(page, size, latestFirstSort()));
        return toPageResult(slice);
    }

    @Override
    public PageResult<ApprovalDocument> findHistoryByApproverId(Long approverId, int page, int size) {
        Slice<ApprovalDocumentEntity> slice = approvalDocumentJpaRepository.findHistoryByApproverId(
                approverId,
                List.of(ApprovalLineStatus.APPROVED, ApprovalLineStatus.REJECTED),
                PageRequest.of(page, size, latestFirstSort()));
        return toPageResult(slice);
    }

    private PageResult<ApprovalDocument> toPageResult(Slice<ApprovalDocumentEntity> slice) {
        List<ApprovalDocument> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    private Sort latestFirstSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt", "id");
    }

    private boolean needsLineReplacement(ApprovalDocument approvalDocument) {
        return approvalDocument.getId() != null
                && approvalDocument.getLines().stream().allMatch(line -> line.getId() == null);
    }

    private ApprovalDocumentEntity updateExistingLines(ApprovalDocument domain) {
        ApprovalDocumentEntity entity = approvalDocumentJpaRepository.getReferenceById(domain.getId());
        entity.clearLines();
        approvalDocumentJpaRepository.saveAndFlush(entity);
        domain.getLines().forEach(line -> entity.addLine(toLineEntity(line)));
        return entity;
    }

    private ApprovalDocumentEntity toEntity(ApprovalDocument domain) {
        ApprovalDocumentEntity entity = ApprovalDocumentEntity.builder()
                .id(domain.getId())
                .templateId(domain.getTemplateId())
                .sourceType(domain.getSourceType())
                .title(domain.getTitle())
                .contentType(domain.getContent().getType())
                .text(domain.getContent().getText())
                .creatorId(domain.getCreatorId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .resubmittedAt(domain.getResubmittedAt())
                .build();

        domain.getLines().forEach(line -> entity.addLine(toLineEntity(line)));
        domain.getAttachments().forEach(attachment -> entity.addAttachment(toAttachmentEntity(attachment)));
        return entity;
    }

    private ApprovalDocumentLineEntity toLineEntity(ApprovalDocumentLine line) {
        return ApprovalDocumentLineEntity.builder()
                .id(line.getId())
                .stepOrder(line.getStepOrder())
                .approverId(line.getApproverId())
                .status(line.getStatus())
                .comment(line.getComment())
                .decidedAt(line.getDecidedAt())
                .build();
    }

    private ApprovalAttachmentEntity toAttachmentEntity(ApprovalAttachment attachment) {
        return ApprovalAttachmentEntity.builder()
                .id(attachment.getId())
                .fileId(attachment.getFileId())
                .aiSummary(attachment.getAiSummary())
                .summaryStatus(attachment.getSummaryStatus())
                .summarizedAt(attachment.getSummarizedAt())
                .build();
    }

    private ApprovalDocument toDomain(ApprovalDocumentEntity entity) {
        List<ApprovalDocumentLine> lines = entity.getLines().stream()
                .map(this::toLineDomain)
                .toList();
        List<ApprovalAttachment> attachments = entity.getAttachments().stream()
                .map(this::toAttachmentDomain)
                .toList();
        ApprovalContent content = ApprovalContent.restore(entity.getContentType(), entity.getText());

        return ApprovalDocument.restore(
                entity.getId(), entity.getTemplateId(), entity.getSourceType(), entity.getTitle(), content,
                entity.getCreatorId(), lines, attachments, entity.getStatus(), entity.getCreatedAt(),
                entity.getResubmittedAt());
    }

    private ApprovalDocumentLine toLineDomain(ApprovalDocumentLineEntity entity) {
        return ApprovalDocumentLine.restore(
                entity.getId(), entity.getStepOrder(), entity.getApproverId(),
                entity.getStatus(), entity.getComment(), entity.getDecidedAt());
    }

    private ApprovalAttachment toAttachmentDomain(ApprovalAttachmentEntity entity) {
        return ApprovalAttachment.restore(
                entity.getId(), entity.getFileId(), entity.getAiSummary(),
                entity.getSummaryStatus(), entity.getSummarizedAt());
    }
}
