package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.exception.task.RecurringTaskTemplateNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecurringTaskTemplatePersistenceAdapter implements RecurringTaskTemplateRepository {

  private final RecurringTaskTemplateJpaRepository recurringTaskTemplateJpaRepository;
  private final RecurringTaskSkipJpaRepository recurringTaskSkipJpaRepository;
  private final WorkspaceJpaRepository workspaceJpaRepository;
  private final RecurringTaskTemplatePersistenceMapper mapper;

  @Override
  public RecurringTaskTemplate save(RecurringTaskTemplate template) {
    if (template.getId() == null) {
      WorkspaceJpaEntity workspace = workspaceJpaRepository.getReferenceById(template.getWorkspaceId());
      RecurringTaskTemplateJpaEntity entity =
          RecurringTaskTemplateJpaEntity.create(
              workspace,
              template.getTitle(),
              template.getRecurrenceType(),
              template.getRecurrenceRule(),
              template.getCreatedBy());
      return mapper.toDomain(recurringTaskTemplateJpaRepository.save(entity));
    }
    RecurringTaskTemplateJpaEntity entity =
        recurringTaskTemplateJpaRepository
            .findById(template.getId())
            .orElseThrow(RecurringTaskTemplateNotFoundException::new);
    entity.changeRecurrence(template.getTitle(), template.getRecurrenceType(), template.getRecurrenceRule());
    return mapper.toDomain(recurringTaskTemplateJpaRepository.save(entity));
  }

  @Override
  public Optional<RecurringTaskTemplate> findByWorkspaceIdAndId(Long workspaceId, Long templateId) {
    return recurringTaskTemplateJpaRepository
        .findByWorkspaceIdAndId(workspaceId, templateId)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<RecurringTaskTemplate> findByWorkspaceIdAndIdForUpdate(Long workspaceId, Long templateId) {
    if (!recurringTaskTemplateJpaRepository.existsByIdAndWorkspaceId(templateId, workspaceId)) {
      return Optional.empty();
    }
    return recurringTaskTemplateJpaRepository.lockById(templateId).map(mapper::toDomain);
  }

  @Override
  public PageResult<RecurringTaskTemplate> findAllByWorkspaceId(Long workspaceId, int page, int size) {
    Slice<RecurringTaskTemplateJpaEntity> slice =
        recurringTaskTemplateJpaRepository.findAllByWorkspaceId(workspaceId, PageRequest.of(page, size));
    List<RecurringTaskTemplate> content = slice.getContent().stream().map(mapper::toDomain).toList();
    return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
  }

  @Override
  public List<RecurringTaskTemplate> findAll() {
    return recurringTaskTemplateJpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public void delete(Long templateId) {
    // 자식 → 부모 순서로 지운다. 운영 MySQL의 ON DELETE CASCADE는 안전망으로 남는다
    // (TaskPersistenceAdapter.delete()와 동일한 패턴).
    recurringTaskSkipJpaRepository.deleteByRecurringTemplateId(templateId);
    recurringTaskTemplateJpaRepository.deleteById(templateId);
  }
}
