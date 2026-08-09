package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.command.task.UpdateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.UpdateRecurringTaskTemplateUseCase;
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
public class UpdateRecurringTaskTemplateService implements UpdateRecurringTaskTemplateUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  @Override
  @Transactional
  public RecurringTaskTemplate update(UpdateRecurringTaskTemplateCommand command) {
    log.info(
        "event=recurring_template_update_시작 workspaceId={}, templateId={}",
        command.workspaceId(),
        command.templateId());

    // 존재 확인을 권한 확인보다 먼저 한다 (기존 워크스페이스 API와 동일한 순서).
    Workspace workspace =
        workspaceRepository.findById(command.workspaceId()).orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    // 삭제 Service와 같은 락(findByWorkspaceIdAndIdForUpdate)을 공유해 동시 요청(같은 워크스페이스·
    // 같은 템플릿)을 직렬화한다 — Task.findByIdForUpdate와 동일 패턴.
    RecurringTaskTemplate template =
        recurringTaskTemplateRepository
            .findByWorkspaceIdAndIdForUpdate(command.workspaceId(), command.templateId())
            .orElseThrow(RecurringTaskTemplateNotFoundException::new);

    String newTitle = command.title() != null ? command.title() : template.getTitle();
    RecurringTaskTemplate changed =
        command.recurrenceType() == null
            ? template.changeRecurrence(newTitle, template.getRecurrenceType(), template.getRecurrenceRule())
            : template.changeRecurrence(newTitle, command.recurrenceType(), command.recurrenceRule());

    RecurringTaskTemplate saved = recurringTaskTemplateRepository.save(changed);

    AfterCommitLogger.run(
        () ->
            log.info(
                "event=recurring_template_update_완료 workspaceId={}, templateId={}",
                command.workspaceId(),
                saved.getId()));
    return saved;
  }
}
