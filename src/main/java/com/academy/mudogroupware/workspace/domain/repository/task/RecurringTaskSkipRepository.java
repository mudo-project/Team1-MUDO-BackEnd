package com.academy.mudogroupware.workspace.domain.repository.task;

import java.time.LocalDateTime;

public interface RecurringTaskSkipRepository {

  // 이미 같은 회차 기록이 있으면 아무 것도 하지 않는다.
  void saveIfAbsent(Long recurringTemplateId, LocalDateTime scheduledFor);

  // 이미 같은 회차 기록이 있는지 확인한다. 생성 스케줄러가 멱등성 체크에 사용한다.
  boolean exists(Long recurringTemplateId, LocalDateTime scheduledFor);
}
