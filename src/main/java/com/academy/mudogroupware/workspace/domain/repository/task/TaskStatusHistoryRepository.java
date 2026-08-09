package com.academy.mudogroupware.workspace.domain.repository.task;

import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TaskStatusHistoryRepository {

  void append(TaskStatusHistory history);

  // 해당 업무의 가장 최근 상태 변경 시각. 이력이 없으면 empty.
  Optional<LocalDateTime> findLatestChangedAt(Long taskId);
}
