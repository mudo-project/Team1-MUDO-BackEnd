package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.usecase.GetMyEmploymentSummaryUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyLeaveSummaryUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyMonthlyAttendanceUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyTodayAttendanceUseCase;

@ExtendWith(MockitoExtension.class)
class MyAttendanceDashboardQueryServiceTest {

    @Mock GetMyMonthlyAttendanceUseCase monthlyUseCase;
    @Mock GetMyTodayAttendanceUseCase todayUseCase;
    @Mock GetMyLeaveSummaryUseCase leaveUseCase;
    @Mock GetMyEmploymentSummaryUseCase employmentUseCase;

    @Test
    void composesExistingQueryResultsWithoutRecalculatingThem() {
        Long userId = 2L;
        Long academyId = 10L;
        var monthly = new MyMonthlyAttendanceView(2026, 8, List.of());
        MyTodayAttendanceView today = null;
        var leave = new MyLeaveSummaryView(15, 3, 2, 10, null);
        MyEmploymentSummaryView employment = null;
        when(monthlyUseCase.getMonthly(userId, academyId, 2026, 8)).thenReturn(monthly);
        when(todayUseCase.getToday(userId, academyId)).thenReturn(today);
        when(leaveUseCase.getSummary(userId, academyId)).thenReturn(leave);
        when(employmentUseCase.getSummary(userId, academyId)).thenReturn(employment);
        var service = new MyAttendanceDashboardQueryService(
                monthlyUseCase, todayUseCase, leaveUseCase, employmentUseCase);

        var result = service.getDashboard(userId, academyId, 2026, 8);

        assertSame(monthly, result.calendar());
        assertSame(today, result.today());
        assertSame(leave, result.leave());
        assertSame(employment, result.employment());
        verify(monthlyUseCase).getMonthly(userId, academyId, 2026, 8);
        verify(todayUseCase).getToday(userId, academyId);
        verify(leaveUseCase).getSummary(userId, academyId);
        verify(employmentUseCase).getSummary(userId, academyId);
    }
}
