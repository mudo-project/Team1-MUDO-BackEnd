package com.academy.mudogroupware.attendance.application.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.GetMyAttendanceDashboardUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyEmploymentSummaryUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyLeaveSummaryUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyMonthlyAttendanceUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyTodayAttendanceUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyAttendanceDashboardQueryService implements GetMyAttendanceDashboardUseCase {

    private final GetMyMonthlyAttendanceUseCase monthlyAttendanceUseCase;
    private final GetMyTodayAttendanceUseCase todayAttendanceUseCase;
    private final GetMyLeaveSummaryUseCase leaveSummaryUseCase;
    private final GetMyEmploymentSummaryUseCase employmentSummaryUseCase;

    @Override
    public MyAttendanceDashboardView getDashboard(
            Long userId, Long academyId, int year, int month) {
        return new MyAttendanceDashboardView(
                monthlyAttendanceUseCase.getMonthly(userId, academyId, year, month),
                todayAttendanceUseCase.getToday(userId, academyId),
                leaveSummaryUseCase.getSummary(userId, academyId),
                employmentSummaryUseCase.getSummary(userId, academyId));
    }
}
