package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringTaskSkipJpaRepository
    extends JpaRepository<RecurringTaskSkipJpaEntity, RecurringTaskSkipId> {

  // 템플릿 하드 삭제 시 자식 행을 먼저 지우기 위한 벌크 삭제다. 운영 MySQL에는
  // ON DELETE CASCADE가 걸려 있지만, @DataJpaTest의 H2 스키마는 엔티티에서 생성되어
  // cascade가 없으므로 명시적으로 지운다.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from RecurringTaskSkipJpaEntity s where s.id.recurringTemplateId = :templateId")
  void deleteByRecurringTemplateId(@Param("templateId") Long templateId);
}
