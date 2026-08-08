package com.academy.mudogroupware.workspace.domain.repository.task;

import java.time.LocalDateTime;

public interface RecurringTaskSkipRepository {

  // 이미 같은 회차 기록이 있으면 아무 것도 하지 않는다.
  void saveIfAbsent(Long recurringTemplateId, LocalDateTime scheduledFor);
}
