package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

public final class AttendancePolicy {

    private final Long id;
    private final LocalTime defaultStartTime;
    private final LocalTime defaultEndTime;
    private final int lateGraceMinutes;
    private final boolean weekdayExceptionEnabled;
    private final List<AttendancePolicyWeekday> weekdays;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AttendancePolicy(Long id,
                             LocalTime defaultStartTime, LocalTime defaultEndTime,
                             int lateGraceMinutes, boolean weekdayExceptionEnabled,
                             List<AttendancePolicyWeekday> weekdays,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        validate(defaultStartTime, defaultEndTime, lateGraceMinutes, weekdays);
        this.id = id;
        this.defaultStartTime = defaultStartTime;
        this.defaultEndTime = defaultEndTime;
        this.lateGraceMinutes = lateGraceMinutes;
        this.weekdayExceptionEnabled = weekdayExceptionEnabled;
        this.weekdays = List.copyOf(weekdays);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AttendancePolicy create(LocalTime defaultStartTime,
                                          LocalTime defaultEndTime,
                                          int lateGraceMinutes,
                                          boolean weekdayExceptionEnabled,
                                          List<AttendancePolicyWeekday> weekdays) {
        LocalDateTime now = LocalDateTime.now();
        return new AttendancePolicy(null, defaultStartTime, defaultEndTime,
                lateGraceMinutes, weekdayExceptionEnabled, weekdays, now, now);
    }

    public static AttendancePolicy restore(Long id,
                                           LocalTime defaultStartTime,
                                           LocalTime defaultEndTime,
                                           int lateGraceMinutes,
                                           boolean weekdayExceptionEnabled,
                                           List<AttendancePolicyWeekday> weekdays,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
        return new AttendancePolicy(id, defaultStartTime, defaultEndTime,
                lateGraceMinutes, weekdayExceptionEnabled, weekdays, createdAt, updatedAt);
    }

    public AttendancePolicy update(LocalTime startTime, LocalTime endTime,
                                   int graceMinutes, boolean exceptionEnabled,
                                   List<AttendancePolicyWeekday> replacementWeekdays) {
        List<AttendancePolicyWeekday> nextWeekdays =
                !exceptionEnabled || replacementWeekdays == null
                        ? weekdays
                        : replacementWeekdays;
        return new AttendancePolicy(id, startTime, endTime,
                graceMinutes, exceptionEnabled, nextWeekdays, createdAt, LocalDateTime.now());
    }

    public boolean isWorkday(int dayOfWeek) {
        if (!weekdayExceptionEnabled) {
            return true;
        }
        return weekdays.stream()
                .filter(weekday -> weekday.dayOfWeek() == dayOfWeek)
                .findFirst()
                .map(AttendancePolicyWeekday::workday)
                .orElse(true);
    }

    private static void validate(LocalTime startTime, LocalTime endTime,
                                 int graceMinutes, List<AttendancePolicyWeekday> weekdays) {
        if (startTime == null || endTime == null
                || startTime.equals(endTime) || graceMinutes < 0 || graceMinutes > 180
                || weekdays == null) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_POLICY);
        }
        Set<Integer> days = new HashSet<>();
        for (AttendancePolicyWeekday weekday : weekdays) {
            if (weekday == null || !days.add(weekday.dayOfWeek())) {
                throw new AttendanceException(AttendanceErrorCode.DUPLICATE_ATTENDANCE_POLICY_WEEKDAY);
            }
        }
    }

    public Long getId() { return id; }
    public LocalTime getDefaultStartTime() { return defaultStartTime; }
    public LocalTime getDefaultEndTime() { return defaultEndTime; }
    public int getLateGraceMinutes() { return lateGraceMinutes; }
    public boolean isWeekdayExceptionEnabled() { return weekdayExceptionEnabled; }
    public List<AttendancePolicyWeekday> getWeekdays() { return weekdays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
