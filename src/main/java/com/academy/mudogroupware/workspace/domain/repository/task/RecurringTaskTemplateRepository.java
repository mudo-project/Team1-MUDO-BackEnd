package com.academy.mudogroupware.workspace.domain.repository.task;

import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import java.util.List;
import java.util.Optional;

public interface RecurringTaskTemplateRepository {

  // id가 null이면 새로 저장하고, 있으면 제목·주기를 반영한다.
  RecurringTaskTemplate save(RecurringTaskTemplate template);

  // workspaceId가 일치하지 않으면 조회 결과가 없다 — 다른 워크스페이스의 templateId로
  // 접근할 수 없도록 조회 자체를 워크스페이스 범위로 제한한다.
  Optional<RecurringTaskTemplate> findByWorkspaceIdAndId(Long workspaceId, Long templateId);

  List<RecurringTaskTemplate> findAllByWorkspaceId(Long workspaceId);

  // 생성 스케줄러가 전체 워크스페이스의 템플릿을 스캔할 때 쓴다.
  List<RecurringTaskTemplate> findAll();

  // 하드 삭제. recurring_task_skip은 DB의 ON DELETE CASCADE로 함께 삭제된다.
  void delete(Long templateId);
}
