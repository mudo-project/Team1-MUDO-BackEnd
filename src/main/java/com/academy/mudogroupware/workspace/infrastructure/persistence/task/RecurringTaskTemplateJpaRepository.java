package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  // 1단계: 락 없이 워크스페이스 소속만 먼저 확인한다. TaskJpaRepository.existsByTaskIdAndWorkspaceId와 동일한 이유
  // — 다른 트랜잭션이 이 행에 배타 락을 걸고 있어도 기다리지 않고, 소속이 아니면 아래 lockById가 실행되지 않는다.
  @Query(
      "select count(t) > 0 from RecurringTaskTemplateJpaEntity t"
          + " where t.id = :templateId and t.workspace.id = :workspaceId")
  boolean existsByIdAndWorkspaceId(
      @Param("templateId") Long templateId, @Param("workspaceId") Long workspaceId);

  // 2단계: 소속이 확인된 templateId에 대해서만 비관적 락을 건다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from RecurringTaskTemplateJpaEntity t where t.id = :templateId")
  Optional<RecurringTaskTemplateJpaEntity> lockById(@Param("templateId") Long templateId);
}
