package com.academy.mudogroupware.workspace.infrastructure.scheduler;

import static org.mockito.Mockito.verify;

import com.academy.mudogroupware.workspace.application.usecase.DelayOverdueTasksUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceTaskDelaySchedulerTest {

  @Mock private DelayOverdueTasksUseCase delayOverdueTasksUseCase;

  @Test
  void delegatesToDelayOverdueTasksUseCase() {
    WorkspaceTaskDelayScheduler scheduler = new WorkspaceTaskDelayScheduler(delayOverdueTasksUseCase);

    scheduler.delayOverdueTasks();

    verify(delayOverdueTasksUseCase).delayOverdueTasks();
  }
}
