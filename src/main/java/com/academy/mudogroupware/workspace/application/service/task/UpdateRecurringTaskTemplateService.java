package com.academy.mudogroupware.workspace.application.service.task;

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

    // 락 없이 조회한다 — 삭제 API가 아직 없어 수정-삭제 동시 경합이 존재하지 않는다.
    // 삭제 API를 추가할 때 findByWorkspaceIdAndIdForUpdate(비관적 락)로 전환하고
    // 두 Service가 그 조회를 공유하도록 바꿔야 한다(Task.findByIdForUpdate와 동일 패턴).
    RecurringTaskTemplate template =
        recurringTaskTemplateRepository
            .findByWorkspaceIdAndId(command.workspaceId(), command.templateId())
            .orElseThrow(RecurringTaskTemplateNotFoundException::new);

    String newTitle = command.title() != null ? command.title() : template.getTitle();
    RecurringTaskTemplate changed =
        command.recurrenceType() == null
            ? template.changeRecurrence(newTitle, template.getRecurrenceType(), template.getRecurrenceRule())
            : template.changeRecurrence(newTitle, command.recurrenceType(), command.recurrenceRule());

    RecurringTaskTemplate saved = recurringTaskTemplateRepository.save(changed);

    log.info(
        "event=recurring_template_update_완료 workspaceId={}, templateId={}",
        command.workspaceId(),
        saved.getId());
    return saved;
  }
}
