package com.academy.mudogroupware.attendance.application.usecase;

import java.time.LocalDate;

import com.academy.mudogroupware.attendance.application.query.WeeklyEmployeeDetailView;

public interface GetWeeklyEmployeeDetailUseCase {
    WeeklyEmployeeDetailView getWeeklyDetail(
            Long requesterId, Long academyId, Long userId, LocalDate date);
}
