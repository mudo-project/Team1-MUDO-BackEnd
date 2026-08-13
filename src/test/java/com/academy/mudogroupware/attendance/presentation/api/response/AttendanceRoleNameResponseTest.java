package com.academy.mudogroupware.attendance.presentation.api.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort.Requester;
import com.academy.mudogroupware.attendance.application.query.AdminAttendanceCorrectionView;
import com.academy.mudogroupware.attendance.application.query.AttendanceCorrectionView;
import com.academy.mudogroupware.attendance.application.query.WeeklyEmployeeDetailView;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;

class AttendanceRoleNameResponseTest {

    @Test
    void weeklyEmployeeDetailUsesRoleName() {
        WeeklyEmployeeDetailView view = new WeeklyEmployeeDetailView(
                new WeeklyEmployeeDetailView.Employee(2L, "employee", "instructor"),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 16),
                5,
                1,
                List.of());

        WeeklyEmployeeDetailResponse response = WeeklyEmployeeDetailResponse.from(view);

        assertEquals("instructor", response.employee().roleName());
    }

    @Test
    void adminCorrectionRequesterUsesRoleName() {
        AttendanceCorrectionView correction = new AttendanceCorrectionView(
                1L,
                LocalDate.of(2026, 8, 11),
                AttendanceCorrectionType.CLOCK_OUT_TIME,
                AttendanceCorrectionStatus.PENDING,
                LocalDateTime.of(2026, 8, 11, 9, 0),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 8, 11, 18, 0),
                null,
                null,
                "clock-out correction",
                LocalDateTime.of(2026, 8, 11, 19, 0),
                null,
                null,
                null);
        AdminAttendanceCorrectionView view = new AdminAttendanceCorrectionView(
                correction, new Requester(2L, "employee", "instructor"));

        AdminAttendanceCorrectionResponse response = AdminAttendanceCorrectionResponse.from(view);

        assertEquals("instructor", response.requester().roleName());
    }
}
