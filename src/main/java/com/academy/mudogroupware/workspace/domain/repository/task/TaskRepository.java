package com.academy.mudogroupware.workspace.domain.repository.task;

import com.academy.mudogroupware.workspace.domain.model.task.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

  // id가 null이면 새로 저장하고, 있으면 상태·마감일을 반영한다.
  Task save(Task task);

  // 수정·삭제 공통 진입점. 비관적 락으로 삭제와 상태 변경의 경합을 막는다.
  // workspaceId가 일치하지 않으면 조회 결과가 없다 — 다른 워크스페이스의 taskId로
  // 락 경합을 일으킬 수 없도록 조회 자체를 워크스페이스 범위로 제한한다.
  Optional<Task> findByIdForUpdate(Long workspaceId, Long taskId);

  // 조회 전용(락 없음). workspaceId가 일치하지 않으면 결과가 없다 — findByIdForUpdate와 동일한 스코프 규칙.
  Optional<Task> findById(Long workspaceId, Long taskId);

  // 하드 삭제. 댓글·멘션·상태 이력을 함께 제거한다.
  void delete(Long taskId);

  // 자동 지연 대상 일반 업무: due_at < today, 상태가 COMPLETED/DELAYED가 아니고
  // 워크스페이스가 소프트 삭제되지 않은 것
  List<Task> findOverdueRegularTasks(LocalDate today);

  // 자동 지연 대상 반복 업무: scheduled_for < 오늘 00:00
  List<Task> findOverdueRecurringTasks(LocalDateTime startOfToday);

  // 반복 업무 생성 스케줄러의 멱등성 체크: 이 회차가 이미 생성됐는지 확인한다.
  boolean existsByRecurringTemplateIdAndScheduledFor(Long recurringTemplateId, LocalDateTime scheduledFor);
}
