package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.command.SaveAttendancePolicyCommand;
import com.academy.mudogroupware.attendance.application.result.SaveAttendancePolicyResult;

public interface SaveAttendancePolicyUseCase {
    SaveAttendancePolicyResult save(SaveAttendancePolicyCommand command);
}
