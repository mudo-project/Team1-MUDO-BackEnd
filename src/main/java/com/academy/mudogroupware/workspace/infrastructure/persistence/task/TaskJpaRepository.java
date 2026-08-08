package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, Long> {

  @Query(
      """
      select t
      from TaskJpaEntity t
      where t.workspace.id = :workspaceId
          and t.recurringTemplate is null
          and (
              t.status <> :completed
              or exists (
                  select 1
                  from TaskStatusHistoryJpaEntity h
                  where h.task = t
                      and h.currentStatus = :completed
                      and h.createdAt >= :startOfDay and h.createdAt < :endOfDay
                      and h.createdAt = (
                          select max(h2.createdAt)
                          from TaskStatusHistoryJpaEntity h2
                          where h2.task = t and h2.currentStatus = :completed)
              )
          )
      """)
  List<TaskJpaEntity> findVisibleRegularTasks(
      @Param("workspaceId") Long workspaceId,
      @Param("startOfDay") LocalDateTime startOfDay,
      @Param("endOfDay") LocalDateTime endOfDay,
      @Param("completed") TaskStatus completed);

  @Query(
      """
      select t
      from TaskJpaEntity t
      where t.workspace.id = :workspaceId
          and t.recurringTemplate is not null
          and t.scheduledFor >= :startOfDay and t.scheduledFor < :endOfDay
      """)
  List<TaskJpaEntity> findVisibleRecurringTasks(
      @Param("workspaceId") Long workspaceId,
      @Param("startOfDay") LocalDateTime startOfDay,
      @Param("endOfDay") LocalDateTime endOfDay);

  @Query(
      """
      select t
      from TaskJpaEntity t
      where t.recurringTemplate is null
          and t.workspace.deletedAt is null
          and t.dueAt < :today
          and t.status not in (:completed, :delayed)
      """)
  List<TaskJpaEntity> findOverdueRegularTasks(
      @Param("today") LocalDate today,
      @Param("completed") TaskStatus completed,
      @Param("delayed") TaskStatus delayed);

  @Query(
      """
      select t
      from TaskJpaEntity t
      where t.recurringTemplate is not null
          and t.workspace.deletedAt is null
          and t.scheduledFor < :startOfToday
          and t.status not in (:completed, :delayed)
      """)
  List<TaskJpaEntity> findOverdueRecurringTasks(
      @Param("startOfToday") LocalDateTime startOfToday,
      @Param("completed") TaskStatus completed,
      @Param("delayed") TaskStatus delayed);

  // 1단계: 락 없이 워크스페이스 소속만 먼저 확인한다. 일반 조회(MVCC 스냅샷)라 다른
  // 트랜잭션이 그 행에 배타 락을 걸고 있어도 기다리지 않는다. 다른 워크스페이스의
  // taskId는 여기서 걸러지므로 아래 lockById가 아예 실행되지 않는다 — 소유하지 않은
  // 업무 행에 대한 락 경합 자체가 발생하지 않는다.
  @Query(
      "select count(t) > 0 from TaskJpaEntity t where t.id = :taskId and t.workspace.id = :workspaceId")
  boolean existsByTaskIdAndWorkspaceId(
      @Param("taskId") Long taskId, @Param("workspaceId") Long workspaceId);

  // 2단계: 소속이 확인된 taskId에 대해서만 비관적 락을 건다. taskId는 PK이므로
  // 단일 행 조회이고, 이 시점에는 이미 워크스페이스 소속이 확인된 뒤라 안전하다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TaskJpaEntity t where t.id = :taskId")
  Optional<TaskJpaEntity> lockById(@Param("taskId") Long taskId);

  // 아래 3개는 업무 하드 삭제 시 자식 행을 먼저 지우기 위한 벌크 삭제다.
  // 운영 MySQL에는 ON DELETE CASCADE가 걸려 있지만, @DataJpaTest의 H2 스키마는
  // 엔티티에서 생성되어 cascade가 없으므로 명시적으로 지운다.
  @Modifying
  @Query(
      """
      delete from TaskCommentMentionJpaEntity m
      where m.comment.id in (select c.id from TaskCommentJpaEntity c where c.task.id = :taskId)
      """)
  void deleteMentionsByTaskId(@Param("taskId") Long taskId);

  @Modifying
  @Query("delete from TaskCommentJpaEntity c where c.task.id = :taskId")
  void deleteCommentsByTaskId(@Param("taskId") Long taskId);

  @Modifying
  @Query("delete from TaskStatusHistoryJpaEntity h where h.task.id = :taskId")
  void deleteStatusHistoriesByTaskId(@Param("taskId") Long taskId);
}
