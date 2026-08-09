package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.command.task.DeleteRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.DeleteRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.domain.exception.task.RecurringTaskTemplateNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteRecurringTaskTemplateService implements DeleteRecurringTaskTemplateUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  @Override
  @Transactional
  public void delete(DeleteRecurringTaskTemplateCommand command) {
    log.info(
        "event=recurring_template_delete_시작 workspaceId={}, templateId={}, requesterId={}",
        command.workspaceId(),
        command.templateId(),
        command.requesterId());

    // 존재 확인을 권한 확인보다 먼저 한다 (기존 워크스페이스 API와 동일한 순서).
    Workspace workspace =
        workspaceRepository.findById(command.workspaceId()).orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    // 수정 Service와 같은 락을 공유한다 — 동시 요청(같은 워크스페이스·같은 템플릿)을 직렬화한다.
    RecurringTaskTemplate template =
        recurringTaskTemplateRepository
            .findByWorkspaceIdAndIdForUpdate(command.workspaceId(), command.templateId())
            .orElseThrow(RecurringTaskTemplateNotFoundException::new);

    // 이미 생성된 Task는 삭제되지 않는다 — recurring_template_id가 NULL이 되어 일반 업무로 남는다
    // (운영 DB의 ON DELETE SET NULL, TaskJpaEntity의 @OnDelete로 H2도 동일하게 동작).
    recurringTaskTemplateRepository.delete(template.getId());

    AfterCommitLogger.run(
        () ->
            log.info(
                "event=recurring_template_delete_완료 workspaceId={}, templateId={}",
                command.workspaceId(),
                template.getId()));
  }
}
