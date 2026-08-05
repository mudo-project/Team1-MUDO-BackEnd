package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;
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
                      and cast(h.createdAt as date) = :date
                      and h.createdAt = (
                          select max(h2.createdAt)
                          from TaskStatusHistoryJpaEntity h2
                          where h2.task = t and h2.currentStatus = :completed)
              )
          )
      """)
  List<TaskJpaEntity> findVisibleRegularTasks(
      @Param("workspaceId") Long workspaceId,
      @Param("date") LocalDate date,
      @Param("completed") TaskStatus completed);
}
