package com.academy.mudogroupware.notice.application.usecase;

import com.academy.mudogroupware.notice.application.command.CreateNoticeCommand;

public interface CreateNoticeUseCase {

    Long createNotice(CreateNoticeCommand command);
}
