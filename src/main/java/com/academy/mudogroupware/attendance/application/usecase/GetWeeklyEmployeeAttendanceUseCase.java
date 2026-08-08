package com.academy.mudogroupware.attendance.application.usecase;

import java.time.LocalDate;

import com.academy.mudogroupware.attendance.application.query.WeeklyEmployeeAttendanceView;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

public interface GetWeeklyEmployeeAttendanceUseCase {
    WeeklyEmployeeAttendanceView getWeekly(Long requesterId, Long academyId, LocalDate date,
                                            String keyword, MyAttendanceDayStatus status,
                                            int page, int size);
}
