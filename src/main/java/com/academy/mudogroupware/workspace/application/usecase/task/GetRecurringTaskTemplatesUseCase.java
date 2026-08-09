package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;

public interface GetRecurringTaskTemplatesUseCase {

  // 최신 생성순으로 정렬한 페이지 결과를 반환한다.
  PageResult<RecurringTaskTemplate> getTemplates(
      Long workspaceId, Long requesterId, int page, int size);
}
