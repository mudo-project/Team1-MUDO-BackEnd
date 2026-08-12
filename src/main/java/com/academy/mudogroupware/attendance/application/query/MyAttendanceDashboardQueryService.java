package com.academy.mudogroupware.attendance.application.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.GetMyAttendanceDashboardUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyEmploymentSummaryUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyLeaveSummaryUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyMonthlyAttendanceUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyTodayAttendanceUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyAttendanceDashboardQueryService implements GetMyAttendanceDashboardUseCase {

    private final GetMyMonthlyAttendanceUseCase monthlyAttendanceUseCase;
    private final GetMyTodayAttendanceUseCase todayAttendanceUseCase;
    private final GetMyLeaveSummaryUseCase leaveSummaryUseCase;
    private final GetMyEmploymentSummaryUseCase employmentSummaryUseCase;

    @Override
    public MyAttendanceDashboardView getDashboard(
            Long userId, int year, int month) {
        log.info("event=attendance_dashboard_read_시작 userId={}, year={}, month={}", userId, year, month);
        try {
        MyAttendanceDashboardView result = new MyAttendanceDashboardView(
                monthlyAttendanceUseCase.getMonthly(userId, year, month),
                todayAttendanceUseCase.getToday(userId),
                leaveSummaryUseCase.getSummary(userId),
                employmentSummaryUseCase.getSummary(userId));
        log.info("event=attendance_dashboard_read_완료 userId={}, year={}, month={}", userId, year, month);
        return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_dashboard_read_실패 userId={}, errorType={}",
                    userId, e.getClass().getSimpleName());
            throw e;
        }
    }
}
