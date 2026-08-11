package com.academy.mudogroupware.rollcall.application.usecase;

import java.util.List;

import com.academy.mudogroupware.rollcall.application.command.SendAttendanceMessagesCommand;
import com.academy.mudogroupware.rollcall.application.query.MessageSendResultView;

public interface SendAttendanceMessagesUseCase {

    List<MessageSendResultView> send(SendAttendanceMessagesCommand command);
}
