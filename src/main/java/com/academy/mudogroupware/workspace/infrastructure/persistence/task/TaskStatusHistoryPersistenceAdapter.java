package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.workspace.domain.model.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.repository.TaskStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TaskStatusHistoryPersistenceAdapter implements TaskStatusHistoryRepository {

  private final TaskStatusHistoryJpaRepository taskStatusHistoryJpaRepository;
  private final TaskJpaRepository taskJpaRepository;

  @Override
  public void append(TaskStatusHistory history) {
    TaskJpaEntity task = taskJpaRepository.getReferenceById(history.getTaskId());
    taskStatusHistoryJpaRepository.save(
        history.getChangedBy() == null
            ? TaskStatusHistoryJpaEntity.systemChanged(
                task, history.getPreviousStatus(), history.getCurrentStatus())
            : TaskStatusHistoryJpaEntity.userChanged(
                task,
                history.getPreviousStatus(),
                history.getCurrentStatus(),
                history.getChangedBy()));
  }
}
