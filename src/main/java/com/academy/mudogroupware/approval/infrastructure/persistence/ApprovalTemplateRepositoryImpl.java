package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalLine;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ApprovalTemplateRepositoryImpl implements ApprovalTemplateRepository {

    private final ApprovalTemplateJpaRepository approvalTemplateJpaRepository;

    @Override
    public ApprovalTemplate save(ApprovalTemplate approvalTemplate) {
        ApprovalTemplateEntity entity = toEntity(approvalTemplate);
        return toDomain(approvalTemplateJpaRepository.save(entity));
    }

    @Override
    public Optional<ApprovalTemplate> findById(Long id) {
        return approvalTemplateJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ApprovalTemplate> findAllByApproverId(Long approverId) {
        return approvalTemplateJpaRepository.findAllByApproverId(approverId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ApprovalTemplateEntity toEntity(ApprovalTemplate domain) {
        ApprovalTemplateEntity entity = ApprovalTemplateEntity.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .contentType(domain.getContent().getType())
                .text(domain.getContent().getText())
                .fileUrl(domain.getContent().getFileUrl())
                .creatorId(domain.getCreatorId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();

        domain.getApprovalLines().forEach(line -> entity.addLine(toLineEntity(line)));
        return entity;
    }

    private ApprovalLineEntity toLineEntity(ApprovalLine line) {
        return ApprovalLineEntity.builder()
                .id(line.getId())
                .stepOrder(line.getStepOrder())
                .approverId(line.getApproverId())
                .status(line.getStatus())
                .comment(line.getComment())
                .decidedAt(line.getDecidedAt())
                .build();
    }

    private ApprovalTemplate toDomain(ApprovalTemplateEntity entity) {
        List<ApprovalLine> lines = entity.getApprovalLines().stream()
                .map(this::toLineDomain)
                .toList();
        ApprovalContent content = ApprovalContent.restore(entity.getContentType(), entity.getText(), entity.getFileUrl());

        return ApprovalTemplate.restore(
                entity.getId(), entity.getTitle(), content, entity.getCreatorId(),
                lines, entity.getStatus(), entity.getCreatedAt());
    }

    private ApprovalLine toLineDomain(ApprovalLineEntity entity) {
        return ApprovalLine.restore(
                entity.getId(), entity.getStepOrder(), entity.getApproverId(),
                entity.getStatus(), entity.getComment(), entity.getDecidedAt());
    }
}
