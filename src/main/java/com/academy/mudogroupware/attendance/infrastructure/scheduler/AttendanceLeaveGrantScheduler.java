package com.academy.mudogroupware.attendance.infrastructure.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.application.usecase.GrantAnnualLeaveUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class AttendanceLeaveGrantScheduler {

    private final GrantAnnualLeaveUseCase grantAnnualLeaveUseCase;
    private final Clock clock;

    @Scheduled(
            cron = "${app.scheduler.attendance-leave-grant.cron:0 5 0 * * *}",
            zone = "${app.scheduler.attendance-leave-grant.zone:Asia/Seoul}"
    )
    public void grantAnnualLeave() {
        LocalDateTime now = LocalDateTime.now(clock);
        int grantedCount = grantAnnualLeaveUseCase.grantAnnualLeave(now);
        log.info("event=attendance_leave_grant_completed grantedCount={} grantDate={}",
                grantedCount, now.toLocalDate());
    }
}
