package com.academy.mudogroupware.workspace.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.academy.mudogroupware.workspace.application.usecase.task.GenerateRecurringTasksUseCase;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class RecurringTaskSchedulerTest {

  @Mock private GenerateRecurringTasksUseCase generateRecurringTasksUseCase;

  @Test
  void delegatesToGenerateRecurringTasksUseCase() {
    RecurringTaskScheduler scheduler = new RecurringTaskScheduler(generateRecurringTasksUseCase);

    scheduler.generateRecurringTasks();

    verify(generateRecurringTasksUseCase).generateRecurringTasks();
  }

  @Test
  void scheduledAnnotationRunsAtKst0005() throws NoSuchMethodException {
    Method method = RecurringTaskScheduler.class.getMethod("generateRecurringTasks");
    Scheduled scheduled = method.getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.cron()).isEqualTo("0 5 0 * * *");
    assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
  }
}
