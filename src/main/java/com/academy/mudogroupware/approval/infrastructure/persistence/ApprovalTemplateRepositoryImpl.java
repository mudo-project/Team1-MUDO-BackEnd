package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplateLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ApprovalTemplateRepositoryImpl implements ApprovalTemplateRepository {

    private final ApprovalTemplateJpaRepository approvalTemplateJpaRepository;

    @Override
    public ApprovalTemplate save(ApprovalTemplate approvalTemplate) {
        ApprovalTemplateEntity entity = approvalTemplate.getId() != null
                ? updateExisting(approvalTemplate)
                : toNewEntity(approvalTemplate);

        return toDomain(approvalTemplateJpaRepository.save(entity));
    }

    @Override
    public Optional<ApprovalTemplate> findById(Long id) {
        return approvalTemplateJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ApprovalTemplate> findAll() {
        return approvalTemplateJpaRepository.findAllByType(ApprovalTemplateEntity.TYPE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        approvalTemplateJpaRepository.deleteById(id);
    }

    private ApprovalTemplateEntity toNewEntity(ApprovalTemplate domain) {
        ApprovalTemplateEntity entity = ApprovalTemplateEntity.builder()
                .academyId(domain.getAcademyId())
                .name(domain.getName())
                .creatorId(domain.getCreatorId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
        domain.getLines().forEach(line -> entity.addLine(toLineEntity(line)));
        return entity;
    }

    private ApprovalTemplateEntity updateExisting(ApprovalTemplate domain) {
        ApprovalTemplateEntity entity = approvalTemplateJpaRepository.getReferenceById(domain.getId());
        entity.setName(domain.getName());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.clearLines();
        domain.getLines().forEach(line -> entity.addLine(toLineEntity(line)));
        return entity;
    }

    private ApprovalTemplateLineEntity toLineEntity(ApprovalTemplateLine line) {
        return ApprovalTemplateLineEntity.builder()
                .stepOrder(line.getStepOrder())
                .approverId(line.getApproverId())
                .roleId(line.getRoleId())
                .build();
    }

    private ApprovalTemplate toDomain(ApprovalTemplateEntity entity) {
        List<ApprovalTemplateLine> lines = entity.getLines().stream()
                .map(line -> ApprovalTemplateLine.restore(line.getId(), line.getStepOrder(), line.getApproverId(),
                        line.getRoleId()))
                .toList();

        return ApprovalTemplate.restore(entity.getId(), entity.getAcademyId(), entity.getName(),
                entity.getCreatorId(), lines, entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
