package com.academy.mudogroupware.rollcall.application.usecase;

import com.academy.mudogroupware.rollcall.application.command.UpdateMessageTemplateCommand;

public interface UpdateMessageTemplateUseCase {

    void updateTemplate(UpdateMessageTemplateCommand command);
}
