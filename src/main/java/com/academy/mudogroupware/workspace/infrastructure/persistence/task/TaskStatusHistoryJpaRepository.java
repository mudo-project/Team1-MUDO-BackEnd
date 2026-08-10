package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskStatusHistoryJpaRepository extends JpaRepository<TaskStatusHistoryJpaEntity, Long> {

  @Query(
      "select h.createdAt from TaskStatusHistoryJpaEntity h"
          + " where h.task.id = :taskId order by h.createdAt desc, h.id desc")
  List<LocalDateTime> findChangedAtOrderByCreatedAtDesc(
      @Param("taskId") Long taskId, Pageable pageable);
}
