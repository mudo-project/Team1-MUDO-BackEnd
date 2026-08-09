package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringTaskTemplateJpaRepository
    extends JpaRepository<RecurringTaskTemplateJpaEntity, Long> {

  @Query(
      "select t from RecurringTaskTemplateJpaEntity t"
          + " where t.id = :templateId and t.workspace.id = :workspaceId")
  Optional<RecurringTaskTemplateJpaEntity> findByWorkspaceIdAndId(
      @Param("workspaceId") Long workspaceId, @Param("templateId") Long templateId);

  @Query(
      "select t from RecurringTaskTemplateJpaEntity t"
          + " where t.workspace.id = :workspaceId order by t.createdAt desc, t.id desc")
  Slice<RecurringTaskTemplateJpaEntity> findAllByWorkspaceId(
      @Param("workspaceId") Long workspaceId, Pageable pageable);
}
