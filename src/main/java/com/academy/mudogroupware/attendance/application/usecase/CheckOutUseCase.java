package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.application.result.CheckOutResult;

public interface CheckOutUseCase {
    CheckOutResult checkOut(CheckOutCommand command);
}
