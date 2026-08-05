package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;
import com.academy.mudogroupware.attendance.application.result.CheckInResult;

public interface CheckInUseCase {
    CheckInResult checkIn(CheckInCommand command);
}
