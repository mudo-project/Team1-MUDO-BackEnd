package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.config.MapStructConfig;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface RecurringTaskTemplatePersistenceMapper {

  default RecurringTaskTemplate toDomain(RecurringTaskTemplateJpaEntity entity) {
    return RecurringTaskTemplate.restore(
        entity.getId(),
        entity.getWorkspace().getId(),
        entity.getTitle(),
        entity.getRecurrenceType(),
        entity.getRecurrenceRule(),
        entity.getCreatedBy());
  }
}
