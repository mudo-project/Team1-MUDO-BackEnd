package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.MyAttendanceDashboardView;

public interface GetMyAttendanceDashboardUseCase {
    MyAttendanceDashboardView getDashboard(Long userId, int year, int month);
}
