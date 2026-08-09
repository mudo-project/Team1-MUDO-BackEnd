package com.academy.mudogroupware.workspace.domain.repository.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import java.util.List;
import java.util.Optional;

public interface RecurringTaskTemplateRepository {

  // id가 null이면 새로 저장하고, 있으면 제목·주기를 반영한다.
  RecurringTaskTemplate save(RecurringTaskTemplate template);

  // workspaceId가 일치하지 않으면 조회 결과가 없다 — 다른 워크스페이스의 templateId로
  // 접근할 수 없도록 조회 자체를 워크스페이스 범위로 제한한다.
  Optional<RecurringTaskTemplate> findByWorkspaceIdAndId(Long workspaceId, Long templateId);

  // Task.findByIdForUpdate와 동일한 2단계 패턴(락 없는 소속 확인 → 비관적 락).
  // 수정·삭제 두 Service가 이 조회를 공유해 동시 요청(같은 워크스페이스·같은 템플릿)을 직렬화한다.
  Optional<RecurringTaskTemplate> findByWorkspaceIdAndIdForUpdate(Long workspaceId, Long templateId);

  // 최신 생성순으로 정렬한 페이지 결과를 반환한다.
  PageResult<RecurringTaskTemplate> findAllByWorkspaceId(Long workspaceId, int page, int size);

  // 생성 스케줄러가 전체 워크스페이스의 템플릿을 스캔할 때 쓴다.
  // 소프트 삭제된 워크스페이스에 속한 템플릿은 제외한다.
  List<RecurringTaskTemplate> findAll();

  // 하드 삭제. recurring_task_skip은 DB의 ON DELETE CASCADE로 함께 삭제된다.
  void delete(Long templateId);
}
