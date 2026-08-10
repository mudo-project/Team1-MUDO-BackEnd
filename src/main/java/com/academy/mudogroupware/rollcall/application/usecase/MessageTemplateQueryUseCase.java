package com.academy.mudogroupware.rollcall.application.usecase;

import java.util.List;

import com.academy.mudogroupware.rollcall.application.query.MessageTemplateView;

public interface MessageTemplateQueryUseCase {

    List<MessageTemplateView> getTemplates(Long academyId);
}
