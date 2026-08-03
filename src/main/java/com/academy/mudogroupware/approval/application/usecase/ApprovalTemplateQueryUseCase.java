package com.academy.mudogroupware.approval.application.usecase;

import java.util.List;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalTemplateSummaryView;

public interface ApprovalTemplateQueryUseCase {

    List<ApprovalTemplateSummaryView> getTemplates(Long requesterId);

    ApprovalTemplateDetailView getTemplateDetail(Long templateId);
}
