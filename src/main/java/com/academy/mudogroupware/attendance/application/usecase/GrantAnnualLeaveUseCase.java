package com.academy.mudogroupware.attendance.application.usecase;

import java.time.LocalDateTime;

public interface GrantAnnualLeaveUseCase {

    int grantAnnualLeave(LocalDateTime now);
}
