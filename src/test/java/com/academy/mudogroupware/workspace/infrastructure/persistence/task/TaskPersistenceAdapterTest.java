package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskPersistenceAdapterTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;

  @Mock private TaskJpaRepository taskJpaRepository;
  @Mock private WorkspaceJpaRepository workspaceJpaRepository;
  @Mock private RecurringTaskTemplateJpaRepository recurringTaskTemplateJpaRepository;
  @Mock private TaskPersistenceMapper taskPersistenceMapper;

  private TaskPersistenceAdapter adapter() {
    return new TaskPersistenceAdapter(
        taskJpaRepository, workspaceJpaRepository, recurringTaskTemplateJpaRepository,
        taskPersistenceMapper);
  }

  @Test
  void doesNotAttemptLockWhenWorkspaceCheckFails() {
    when(taskJpaRepository.existsByTaskIdAndWorkspaceId(TASK_ID, WORKSPACE_ID)).thenReturn(false);

    Optional<Task> result = adapter().findByIdForUpdate(WORKSPACE_ID, TASK_ID);

    assertThat(result).isEmpty();
    verify(taskJpaRepository, never()).lockById(any());
  }

  @Test
  void locksByIdOnlyAfterWorkspaceCheckPasses() {
    TaskJpaEntity entity = mock(TaskJpaEntity.class);
    Task mapped = mock(Task.class);
    when(taskJpaRepository.existsByTaskIdAndWorkspaceId(TASK_ID, WORKSPACE_ID)).thenReturn(true);
    when(taskJpaRepository.lockById(TASK_ID)).thenReturn(Optional.of(entity));
    when(taskPersistenceMapper.toDomain(entity)).thenReturn(mapped);

    Optional<Task> result = adapter().findByIdForUpdate(WORKSPACE_ID, TASK_ID);

    assertThat(result).contains(mapped);
    verify(taskJpaRepository).lockById(TASK_ID);
  }
}
