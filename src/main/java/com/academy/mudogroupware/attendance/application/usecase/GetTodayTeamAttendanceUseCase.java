package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.TodayTeamAttendanceView;

public interface GetTodayTeamAttendanceUseCase {
    TodayTeamAttendanceView getToday(Long requesterId);
}
