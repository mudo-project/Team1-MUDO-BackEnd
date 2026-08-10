package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.MyTodayAttendanceView;

public interface GetMyTodayAttendanceUseCase {
    MyTodayAttendanceView getToday(Long userId);
}
