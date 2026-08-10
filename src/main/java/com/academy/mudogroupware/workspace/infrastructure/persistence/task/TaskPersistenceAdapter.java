package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TaskPersistenceAdapter implements TaskRepository {

  private final TaskJpaRepository taskJpaRepository;
  private final WorkspaceJpaRepository workspaceJpaRepository;
  private final RecurringTaskTemplateJpaRepository recurringTaskTemplateJpaRepository;
  private final TaskPersistenceMapper taskPersistenceMapper;

  @Override
  public Task save(Task task) {
    if (task.getId() == null) {
      return taskPersistenceMapper.toDomain(taskJpaRepository.saveAndFlush(newEntity(task)));
    }
    TaskJpaEntity entity =
        taskJpaRepository.findById(task.getId()).orElseThrow(TaskNotFoundException::new);
    entity.updateStatusAndDueAt(task.getStatus(), task.getDueAt());
    return taskPersistenceMapper.toDomain(taskJpaRepository.save(entity));
  }

  @Override
  public Optional<Task> findByIdForUpdate(Long workspaceId, Long taskId) {
    // 1단계: 락 없이 워크스페이스 소속을 먼저 확인한다. 다른 워크스페이스의 taskId는
    // 여기서 걸러지므로 아래 lockById가 호출되지 않고, 실제 락 경합이 발생하지 않는다.
    if (!taskJpaRepository.existsByTaskIdAndWorkspaceId(taskId, workspaceId)) {
      return Optional.empty();
    }
    // 2단계: 소속이 확인된 taskId에 대해서만 비관적 락을 건다.
    return taskJpaRepository.lockById(taskId).map(taskPersistenceMapper::toDomain);
  }

  @Override
  public Optional<Task> findById(Long workspaceId, Long taskId) {
    return taskJpaRepository
        .findByIdAndWorkspaceId(taskId, workspaceId)
        .map(taskPersistenceMapper::toDomain);
  }

  @Override
  public void delete(Long taskId) {
    // 자식 → 부모 순서로 지운다. 운영 MySQL의 ON DELETE CASCADE는 안전망으로 남는다.
    taskJpaRepository.deleteMentionsByTaskId(taskId);
    taskJpaRepository.deleteCommentsByTaskId(taskId);
    taskJpaRepository.deleteStatusHistoriesByTaskId(taskId);
    taskJpaRepository.deleteById(taskId);
    taskJpaRepository.flush();
  }

  @Override
  public List<Task> findOverdueRegularTasks(LocalDate today) {
    return taskJpaRepository
        .findOverdueRegularTasks(today, TaskStatus.COMPLETED, TaskStatus.DELAYED)
        .stream()
        .map(taskPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Task> findOverdueRecurringTasks(LocalDateTime startOfToday) {
    return taskJpaRepository
        .findOverdueRecurringTasks(startOfToday, TaskStatus.COMPLETED, TaskStatus.DELAYED)
        .stream()
        .map(taskPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public boolean existsByRecurringTemplateIdAndScheduledFor(
      Long recurringTemplateId, LocalDateTime scheduledFor) {
    return taskJpaRepository.existsByRecurringTemplate_IdAndScheduledFor(
        recurringTemplateId, scheduledFor);
  }

  private TaskJpaEntity newEntity(Task task) {
    WorkspaceJpaEntity workspace = workspaceJpaRepository.getReferenceById(task.getWorkspaceId());
    RecurringTaskTemplateJpaEntity recurringTemplate =
        task.getRecurringTemplateId() == null
            ? null
            : recurringTaskTemplateJpaRepository.getReferenceById(task.getRecurringTemplateId());
    return taskPersistenceMapper.toEntity(task, workspace, recurringTemplate);
  }
}
