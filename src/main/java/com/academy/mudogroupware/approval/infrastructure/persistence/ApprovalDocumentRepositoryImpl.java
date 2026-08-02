package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ApprovalDocumentRepositoryImpl implements ApprovalDocumentRepository {

    private final ApprovalDocumentJpaRepository approvalDocumentJpaRepository;

    @Override
    public ApprovalDocument save(ApprovalDocument approvalDocument) {
        ApprovalDocumentEntity entity = toEntity(approvalDocument);
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
    public List<ApprovalDocument> findAllByCreatorId(Long creatorId) {
        return approvalDocumentJpaRepository.findAllByCreatorId(creatorId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ApprovalDocumentEntity toEntity(ApprovalDocument domain) {
        ApprovalDocumentEntity entity = ApprovalDocumentEntity.builder()
                .id(domain.getId())
                .templateId(domain.getTemplateId())
                .title(domain.getTitle())
                .contentType(domain.getContent().getType())
                .text(domain.getContent().getText())
                .fileUrl(domain.getContent().getFileUrl())
                .creatorId(domain.getCreatorId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();

        domain.getLines().forEach(line -> entity.addLine(toLineEntity(line)));
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

    private ApprovalDocument toDomain(ApprovalDocumentEntity entity) {
        List<ApprovalDocumentLine> lines = entity.getLines().stream()
                .map(this::toLineDomain)
                .toList();
        ApprovalContent content = ApprovalContent.restore(entity.getContentType(), entity.getText(), entity.getFileUrl());

        return ApprovalDocument.restore(
                entity.getId(), entity.getTemplateId(), entity.getTitle(), content, entity.getCreatorId(),
                lines, entity.getStatus(), entity.getCreatedAt());
    }

    private ApprovalDocumentLine toLineDomain(ApprovalDocumentLineEntity entity) {
        return ApprovalDocumentLine.restore(
                entity.getId(), entity.getStepOrder(), entity.getApproverId(),
                entity.getStatus(), entity.getComment(), entity.getDecidedAt());
    }
}
