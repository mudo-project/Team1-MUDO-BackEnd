package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.GetMyTodayAttendanceUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyTodayAttendanceQueryService implements GetMyTodayAttendanceUseCase {

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final MyAttendanceScheduleResolver scheduleResolver;
    private final MyAttendanceStatusResolver statusResolver;
    private final Clock clock;

    @Override
    public MyTodayAttendanceView getToday(Long userId) {
        log.info("event=attendance_today_read_시작 userId={}={}", userId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        var policy = attendancePolicyRepository.findCurrent()
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));
        var schedule = scheduleResolver.resolve(policy, today);
        AttendanceRecord record = attendanceRecordRepository
                .findByUserIdAndWorkDate(userId, today)
                .orElse(null);
        var approvedLeaves = leaveRequestRepository.findApprovedOverlapping(
                userId, today, today);
        var status = statusResolver.resolve(
                today, schedule, record, approvedLeaves, today, currentTime);

        MyTodayAttendanceView result = new MyTodayAttendanceView(
                today,
                schedule.startTime(),
                schedule.endTime(),
                toOffset(record == null ? null : record.getClockInAt()),
                toOffset(record == null ? null : record.getClockOutAt()),
                status,
                now.atZone(clock.getZone()).toOffsetDateTime());
        log.info("event=attendance_today_read_완료 userId={}={}, status={}", userId, status);
        return result;
    }

    private java.time.OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime == null
                ? null : dateTime.atZone(clock.getZone()).toOffsetDateTime();
    }
}
