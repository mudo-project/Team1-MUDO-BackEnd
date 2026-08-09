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
            Long userId, Long academyId, int year, int month) {
        log.info("event=attendance_dashboard_read_시작 userId={}, academyId={}, year={}, month={}", userId, academyId, year, month);
        MyAttendanceDashboardView result = new MyAttendanceDashboardView(
                monthlyAttendanceUseCase.getMonthly(userId, academyId, year, month),
                todayAttendanceUseCase.getToday(userId, academyId),
                leaveSummaryUseCase.getSummary(userId, academyId),
                employmentSummaryUseCase.getSummary(userId, academyId));
        log.info("event=attendance_dashboard_read_완료 userId={}, academyId={}, year={}, month={}", userId, academyId, year, month);
        return result;
    }
}
