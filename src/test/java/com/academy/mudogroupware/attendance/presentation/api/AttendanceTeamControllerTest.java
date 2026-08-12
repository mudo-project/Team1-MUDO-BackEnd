package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.application.query.TodayTeamAttendanceView;
import com.academy.mudogroupware.attendance.application.usecase.GetTodayTeamAttendanceUseCase;
import com.academy.mudogroupware.attendance.domain.model.TeamAttendanceStatus;
import com.academy.mudogroupware.attendance.presentation.api.response.TodayTeamAttendanceResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

class AttendanceTeamControllerTest {

    @Test
    void getsTodayTeamAttendanceWithAuthenticatedUser() {
        GetTodayTeamAttendanceUseCase useCase = mock(GetTodayTeamAttendanceUseCase.class);
        AttendanceTeamController controller = new AttendanceTeamController(useCase);
        AuthUser authUser = new AuthUser(1L, "owner", 2L, "OWNER");
        TodayTeamAttendanceView view = new TodayTeamAttendanceView(
                LocalDate.of(2026, 8, 5),
                "수",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                new TodayTeamAttendanceView.Summary(1, 0, 0, 0),
                List.of(new TodayTeamAttendanceView.Employee(
                        2L, "김지수", TeamAttendanceStatus.PRESENT, LocalTime.of(8, 52))));
        when(useCase.getToday(1L)).thenReturn(view);

        GlobalApiResponse<TodayTeamAttendanceResponse> response = controller.getToday(authUser);

        verify(useCase).getToday(1L);
        assertEquals("ATTENDANCE_200_3", response.code());
        assertEquals(1, response.data().summary().presentCount());
        assertEquals("김지수", response.data().employees().get(0).name());
    }
}
