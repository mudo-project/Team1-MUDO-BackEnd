package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.command.SubmitLeaveRequestCommand;

public interface SubmitLeaveRequestUseCase {

    void submit(SubmitLeaveRequestCommand command);
}
