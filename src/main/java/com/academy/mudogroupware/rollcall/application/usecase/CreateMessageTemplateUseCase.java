package com.academy.mudogroupware.rollcall.application.usecase;

import com.academy.mudogroupware.rollcall.application.command.CreateMessageTemplateCommand;

public interface CreateMessageTemplateUseCase {

    Long createTemplate(CreateMessageTemplateCommand command);
}
