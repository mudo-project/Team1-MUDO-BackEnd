package com.academy.mudogroupware.notice.application.usecase;

import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;

public interface UpdateNoticeUseCase {

    void updateNotice(UpdateNoticeCommand command);
}
