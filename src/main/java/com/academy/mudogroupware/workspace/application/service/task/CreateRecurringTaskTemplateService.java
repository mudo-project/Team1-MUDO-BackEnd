package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateRecurringTaskTemplateUseCase;
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
public class CreateRecurringTaskTemplateService implements CreateRecurringTaskTemplateUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  @Override
  @Transactional
  public Long create(CreateRecurringTaskTemplateCommand command) {
    log.info(
        "event=recurring_template_create_시작 workspaceId={}, title={}",
        command.workspaceId(),
        command.title());

    // 존재 확인을 권한 확인보다 먼저 한다 (기존 워크스페이스 API와 동일한 순서).
    Workspace workspace =
        workspaceRepository.findById(command.workspaceId()).orElseThrow(WorkspaceNotFoundException::new);

    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                command.workspaceId(),
                command.title(),
                command.recurrenceType(),
                command.recurrenceRule(),
                command.requesterId()));

    AfterCommitLogger.run(
        () ->
            log.info(
                "event=recurring_template_create_완료 workspaceId={}, templateId={}",
                command.workspaceId(),
                saved.getId()));
    return saved.getId();
  }
}
