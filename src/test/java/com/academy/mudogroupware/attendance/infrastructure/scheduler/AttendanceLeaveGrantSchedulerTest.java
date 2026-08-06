package com.academy.mudogroupware.attendance.infrastructure.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.usecase.GrantAnnualLeaveUseCase;

@ExtendWith(MockitoExtension.class)
class AttendanceLeaveGrantSchedulerTest {

    @Mock
    private GrantAnnualLeaveUseCase grantAnnualLeaveUseCase;

    @Test
    void passesClockTimeToUseCase() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-05T15:05:00Z"), ZoneId.of("Asia/Seoul"));
        LocalDateTime expected = LocalDateTime.of(2026, 8, 6, 0, 5);
        when(grantAnnualLeaveUseCase.grantAnnualLeave(expected)).thenReturn(2);
        AttendanceLeaveGrantScheduler scheduler =
                new AttendanceLeaveGrantScheduler(grantAnnualLeaveUseCase, clock);

        scheduler.grantAnnualLeave();

        verify(grantAnnualLeaveUseCase).grantAnnualLeave(expected);
    }
}
