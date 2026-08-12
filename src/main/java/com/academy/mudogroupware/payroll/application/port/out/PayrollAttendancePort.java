package com.academy.mudogroupware.payroll.application.port.out;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.List;

public interface PayrollAttendancePort {
  AttendanceResult getMonthlyAttendance(Long userId, YearMonth yearMonth);

  record AttendanceResult(
      int workDays,
      long workMinutes,
      long overtimeMinutes,
      long nightMinutes,
      long holidayMinutes,
      long paidLeaveMinutes,
      List<WeeklyHoliday> weeklyHolidays,
      List<DailyAttendance> daily) {
    public BigDecimal workHours() { return hours(workMinutes); }
    public BigDecimal overtimeHours() { return hours(overtimeMinutes); }
    public BigDecimal nightHours() { return hours(nightMinutes); }
    public BigDecimal holidayHours() { return hours(holidayMinutes); }
    public BigDecimal paidLeaveHours() { return hours(paidLeaveMinutes); }
    public BigDecimal weeklyHolidayHours() {
      return hours(weeklyHolidays.stream().mapToLong(WeeklyHoliday::minutes).sum());
    }
    private BigDecimal hours(long minutes) {
      return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
    }
  }

  record DailyAttendance(LocalDate date, long regularWorkMinutes, long overtimeMinutes,
      long nightMinutes, long holidayMinutes) {}
  record WeeklyHoliday(LocalDate date, long minutes) {}
}
