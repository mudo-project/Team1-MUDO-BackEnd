package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
