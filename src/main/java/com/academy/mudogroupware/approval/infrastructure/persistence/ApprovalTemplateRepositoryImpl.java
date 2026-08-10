package com.academy.mudogroupware.approval.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplateLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

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
        return approvalTemplateJpaRepository.findByIdAndType(id, ApprovalTemplateEntity.TYPE).map(this::toDomain);
    }

    @Override
    public List<ApprovalTemplate> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return approvalTemplateJpaRepository.findAllByIdInAndType(ids, ApprovalTemplateEntity.TYPE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<ApprovalTemplate> findAll(Long academyId, int page, int size) {
        Slice<ApprovalTemplateEntity> slice = approvalTemplateJpaRepository.findAllByTypeAndAcademyId(
                ApprovalTemplateEntity.TYPE, academyId, PageRequest.of(page, size, latestFirstSort()));
        List<ApprovalTemplate> content = slice.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    private Sort latestFirstSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt", "id");
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
                .build();
        domain.getLines().forEach(line -> entity.addLine(toLineEntity(line)));
        return entity;
    }

    private ApprovalTemplateEntity updateExisting(ApprovalTemplate domain) {
        ApprovalTemplateEntity entity = approvalTemplateJpaRepository.getReferenceById(domain.getId());
        entity.setName(domain.getName());
        entity.clearLines();
        // orphanRemoval 컬렉션을 비우자마자 다시 채우면, Hibernate가 같은 flush 안에서
        // 새 라인 INSERT를 기존 라인 DELETE보다 먼저 실행해 (template_id, step_order)
        // 유니크 제약(uk_approval_line_step_template_step)에 걸린다. 삭제를 먼저 flush한다.
        approvalTemplateJpaRepository.saveAndFlush(entity);
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
