package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaskJpaEntityTest {

  @Test
  void updateStatusAndDueAtAppliesBothFields() {
    TaskJpaEntity task =
        TaskJpaEntity.create(
            null, null, "제목", 10L, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 1), null);

    task.updateStatusAndDueAt(TaskStatus.DELAYED, LocalDate.of(2026, 8, 20));

    assertThat(task.getStatus()).isEqualTo(TaskStatus.DELAYED);
    assertThat(task.getDueAt()).isEqualTo(LocalDate.of(2026, 8, 20));
  }
}
