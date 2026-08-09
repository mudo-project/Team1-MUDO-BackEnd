package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecurringTaskSkipPersistenceAdapter implements RecurringTaskSkipRepository {

  private final RecurringTaskSkipJpaRepository recurringTaskSkipJpaRepository;
  private final RecurringTaskTemplateJpaRepository recurringTaskTemplateJpaRepository;

  @Override
  public void saveIfAbsent(Long recurringTemplateId, LocalDateTime scheduledFor) {
    RecurringTaskSkipId id = RecurringTaskSkipId.of(recurringTemplateId, scheduledFor);
    if (recurringTaskSkipJpaRepository.existsById(id)) {
      return;
    }
    RecurringTaskTemplateJpaEntity template =
        recurringTaskTemplateJpaRepository.getReferenceById(recurringTemplateId);
    recurringTaskSkipJpaRepository.save(
        RecurringTaskSkipJpaEntity.create(template, scheduledFor));
  }

  @Override
  public boolean exists(Long recurringTemplateId, LocalDateTime scheduledFor) {
    return recurringTaskSkipJpaRepository.existsById(
        RecurringTaskSkipId.of(recurringTemplateId, scheduledFor));
  }
}
