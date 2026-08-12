package com.academy.mudogroupware.attendance.infrastructure.payroll;

import com.academy.mudogroupware.attendance.domain.model.*;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollAttendancePort;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollAttendanceAdapter implements PayrollAttendancePort {
  private final AttendanceRecordRepository recordRepository;
  private final AttendancePolicyRepository policyRepository;
  private final LeaveRequestRepository leaveRepository;

  /** Consumer: payroll. Purpose: 월 급여 계산을 위한 근태 분 단위 집계. */
  @Override
  public AttendanceResult getMonthlyAttendance(Long userId, YearMonth month) {
    AttendancePolicy policy = policyRepository.findCurrent().orElseThrow(
        () -> new PayrollException(PayrollErrorCode.PAYROLL_REFERENCE_DATA_MISSING,
            "근태 정책이 없습니다."));
    LocalDate monthStart = month.atDay(1);
    LocalDate monthEnd = month.atEndOfMonth();
    LocalDate queryStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate queryEnd = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    List<AttendanceRecord> records = recordRepository.findByUserIdAndWorkDateBetween(
        userId, queryStart, queryEnd);
    List<LeaveRequest> leaves = leaveRepository.findApprovedOverlapping(userId, queryStart, queryEnd);

    long work = 0;
    long overtime = 0;
    long night = 0;
    long holiday = 0;
    int workDays = 0;
    List<DailyAttendance> daily = new ArrayList<>();
    for (AttendanceRecord record : records) {
      if (record.getWorkDate().isBefore(monthStart) || record.getWorkDate().isAfter(monthEnd)
          || record.getClockOutAt() == null) continue;
      long actual = ChronoUnit.MINUTES.between(record.getClockInAt(), record.getClockOutAt());
      night += nightMinutes(record.getClockInAt(), record.getClockOutAt());
      if (!isWorkday(policy, record.getWorkDate())) {
        holiday += actual;
        daily.add(new DailyAttendance(record.getWorkDate(), 0, actual));
        continue;
      }
      workDays++;
      long recordOvertime = 0;
      if (record.getClockOutType() == ClockOutType.OVERTIME) {
        LocalDateTime scheduledEnd = scheduledEnd(policy, record.getWorkDate());
        if (record.getClockOutAt().isAfter(scheduledEnd)) {
          recordOvertime = ChronoUnit.MINUTES.between(scheduledEnd, record.getClockOutAt());
        }
      }
      overtime += recordOvertime;
      work += Math.max(0, actual - recordOvertime);
      daily.add(new DailyAttendance(record.getWorkDate(), Math.max(0, actual - recordOvertime), 0));
    }

    long paidLeave = 0;
    Set<LocalDate> paidLeaveDates = approvedDates(leaves);
    for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
      if (paidLeaveDates.contains(date) && isWorkday(policy, date)) paidLeave += scheduledMinutes(policy, date);
    }
    long weeklyHoliday = weeklyHolidayMinutes(policy, records, paidLeaveDates, queryStart, queryEnd,
        monthStart, monthEnd);
    return new AttendanceResult(workDays, work, overtime, night, holiday, paidLeave, weeklyHoliday,
        List.copyOf(daily));
  }

  private long weeklyHolidayMinutes(AttendancePolicy policy, List<AttendanceRecord> records,
      Set<LocalDate> paidLeaves, LocalDate start, LocalDate end,
      LocalDate monthStart, LocalDate monthEnd) {
    Set<LocalDate> completed = new HashSet<>();
    records.stream().filter(r -> r.getClockOutAt() != null).map(AttendanceRecord::getWorkDate)
        .forEach(completed::add);
    long total = 0;
    for (LocalDate monday = start; !monday.isAfter(end); monday = monday.plusWeeks(1)) {
      LocalDate sunday = monday.plusDays(6);
      if (sunday.isBefore(monthStart) || sunday.isAfter(monthEnd)) continue;
      int scheduledDays = 0;
      long scheduledMinutes = 0;
      boolean fulfilled = true;
      for (int i = 0; i < 7; i++) {
        LocalDate date = monday.plusDays(i);
        if (!isWorkday(policy, date)) continue;
        scheduledDays++;
        scheduledMinutes += scheduledMinutes(policy, date);
        if (!completed.contains(date) && !paidLeaves.contains(date)) fulfilled = false;
      }
      if (fulfilled && scheduledDays > 0 && scheduledMinutes >= 15 * 60L) {
        total += scheduledMinutes / scheduledDays;
      }
    }
    return total;
  }

  private Set<LocalDate> approvedDates(List<LeaveRequest> leaves) {
    Set<LocalDate> dates = new HashSet<>();
    for (LeaveRequest leave : leaves) {
      if (leave.getStatus() != LeaveRequestStatus.APPROVED) continue;
      for (LocalDate date = leave.getStartDate(); !date.isAfter(leave.getEndDate()); date = date.plusDays(1)) {
        dates.add(date);
      }
    }
    return dates;
  }

  private boolean isWorkday(AttendancePolicy policy, LocalDate date) {
    return policy.isWorkday(date.getDayOfWeek().getValue());
  }

  private long scheduledMinutes(AttendancePolicy policy, LocalDate date) {
    LocalTime start = policy.getDefaultStartTime();
    LocalTime end = policy.getDefaultEndTime();
    if (policy.isWeekdayExceptionEnabled()) {
      AttendancePolicyWeekday weekday = policy.getWeekdays().stream()
          .filter(w -> w.dayOfWeek() == date.getDayOfWeek().getValue()).findFirst().orElse(null);
      if (weekday != null && weekday.workday() && weekday.startTime() != null) {
        start = weekday.startTime();
        end = weekday.endTime();
      }
    }
    LocalDateTime startAt = date.atTime(start);
    LocalDateTime endAt = date.atTime(end);
    if (!endAt.isAfter(startAt)) endAt = endAt.plusDays(1);
    return ChronoUnit.MINUTES.between(startAt, endAt);
  }

  private LocalDateTime scheduledEnd(AttendancePolicy policy, LocalDate date) {
    LocalTime start = policy.getDefaultStartTime();
    LocalTime end = policy.getDefaultEndTime();
    if (policy.isWeekdayExceptionEnabled()) {
      AttendancePolicyWeekday weekday = policy.getWeekdays().stream()
          .filter(w -> w.dayOfWeek() == date.getDayOfWeek().getValue() && w.workday())
          .findFirst().orElse(null);
      if (weekday != null && weekday.startTime() != null) {
        start = weekday.startTime();
        end = weekday.endTime();
      }
    }
    LocalDateTime result = date.atTime(end);
    if (!end.isAfter(start)) result = result.plusDays(1);
    return result;
  }

  private long nightMinutes(LocalDateTime start, LocalDateTime end) {
    long total = 0;
    for (LocalDate date = start.toLocalDate().minusDays(1);
         !date.isAfter(end.toLocalDate()); date = date.plusDays(1)) {
      LocalDateTime nightStart = date.atTime(22, 0);
      LocalDateTime nightEnd = date.plusDays(1).atTime(6, 0);
      LocalDateTime overlapStart = start.isAfter(nightStart) ? start : nightStart;
      LocalDateTime overlapEnd = end.isBefore(nightEnd) ? end : nightEnd;
      if (overlapEnd.isAfter(overlapStart)) {
        total += ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
      }
    }
    return total;
  }
}
