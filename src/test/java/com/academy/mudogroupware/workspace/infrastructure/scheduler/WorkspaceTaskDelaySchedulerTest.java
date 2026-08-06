package com.academy.mudogroupware.workspace.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.academy.mudogroupware.workspace.application.usecase.DelayOverdueTasksUseCase;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class WorkspaceTaskDelaySchedulerTest {

  @Mock private DelayOverdueTasksUseCase delayOverdueTasksUseCase;

  @Test
  void delegatesToDelayOverdueTasksUseCase() {
    WorkspaceTaskDelayScheduler scheduler = new WorkspaceTaskDelayScheduler(delayOverdueTasksUseCase);

    scheduler.delayOverdueTasks();

    verify(delayOverdueTasksUseCase).delayOverdueTasks();
  }

  @Test
  void scheduledAnnotationRunsAtKstMidnight() throws NoSuchMethodException {
    Method method = WorkspaceTaskDelayScheduler.class.getMethod("delayOverdueTasks");
    Scheduled scheduled = method.getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.cron()).isEqualTo("0 0 0 * * *");
    assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
  }
}
