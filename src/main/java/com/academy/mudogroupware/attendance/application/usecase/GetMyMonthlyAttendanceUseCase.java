package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.MyMonthlyAttendanceView;

public interface GetMyMonthlyAttendanceUseCase {
    MyMonthlyAttendanceView getMonthly(Long userId, int year, int month);
}
