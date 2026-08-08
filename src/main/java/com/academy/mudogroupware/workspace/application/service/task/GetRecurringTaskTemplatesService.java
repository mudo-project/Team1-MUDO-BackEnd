package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.usecase.task.GetRecurringTaskTemplatesUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecurringTaskTemplatesService implements GetRecurringTaskTemplatesUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  @Override
  public PageResult<RecurringTaskTemplate> getTemplates(
      Long workspaceId, Long requesterId, int page, int size) {
    // 존재 확인을 권한 확인보다 먼저 한다 (기존 워크스페이스 API와 동일한 순서).
    Workspace workspace =
        workspaceRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:READ 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(requesterId)) {
      throw new WorkspaceAccessDeniedException();
    }

    return recurringTaskTemplateRepository.findAllByWorkspaceId(workspaceId, page, size);
  }
}
