package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.config.MapStructConfig;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface TaskPersistenceMapper {

  default Task toDomain(TaskJpaEntity entity) {
    return Task.restore(
        entity.getId(),
        entity.getWorkspace().getId(),
        entity.getRecurringTemplate() == null ? null : entity.getRecurringTemplate().getId(),
        entity.getTitle(),
        entity.getStatus(),
        entity.getDueAt(),
        entity.getScheduledFor(),
        entity.getCreatedBy(),
        entity.getCreatedAt());
  }

  // workspace / recurringTemplate 참조는 어댑터가 해결해서 넘긴다.
  default TaskJpaEntity toEntity(
      Task task, WorkspaceJpaEntity workspace, RecurringTaskTemplateJpaEntity recurringTemplate) {
    return TaskJpaEntity.create(
        workspace,
        recurringTemplate,
        task.getTitle(),
        task.getCreatedBy(),
        task.getStatus(),
        task.getDueAt(),
        task.getScheduledFor());
  }
}
