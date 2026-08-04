package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalTemplateSummaryView;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public interface ApprovalTemplateQueryUseCase {

    PageResult<ApprovalTemplateSummaryView> getTemplates(Long requesterId, int page, int size);

    ApprovalTemplateDetailView getTemplateDetail(Long templateId);
}
