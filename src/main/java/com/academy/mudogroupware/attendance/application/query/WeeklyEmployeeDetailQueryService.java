package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.WeeklyEmployeeDetail;
import com.academy.mudogroupware.attendance.application.port.WeeklyEmployeeDetailQueryPort;
import com.academy.mudogroupware.attendance.application.usecase.GetWeeklyEmployeeDetailUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WeeklyEmployeeDetailQueryService implements GetWeeklyEmployeeDetailUseCase {

    private final AttendancePolicyRepository policyRepository;
    private final WeeklyEmployeeDetailQueryPort detailQueryPort;
    private final LeaveRequestRepository leaveRequestRepository;
    private final MyAttendanceScheduleResolver scheduleResolver;
    private final Clock clock;

    @Override
    public WeeklyEmployeeDetailView getWeeklyDetail(
            Long requesterId, Long userId, LocalDate date) {
        log.info("event=attendance_employee_weekly_detail_read_시작 requesterId={}, userId={}, date={}", requesterId, userId, date);
        try {
        if (date == null || userId == null) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_QUERY_PERIOD);
        }
        AttendancePolicy policy = policyRepository.findCurrent()
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));
        LocalDate startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = startDate.plusDays(6);
        List<WeeklyEmployeeDetail> rows = detailQueryPort.findByEmployee(
                userId, startDate, endDate);
        if (rows.isEmpty()) {
            throw new AttendanceException(AttendanceErrorCode.ATTENDANCE_EMPLOYEE_NOT_FOUND);
        }

        Map<LocalDate, WeeklyEmployeeDetail> records = rows.stream()
                .filter(row -> row.workDate() != null)
                .collect(Collectors.toMap(WeeklyEmployeeDetail::workDate,
                        Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<LocalDate, MyAttendanceScheduleResolver.WorkSchedule> schedules = new LinkedHashMap<>();
        Map<LocalDate, Set<Long>> approvedLeaves =
                leaveRequestRepository.findApprovedUserIdsBetween(startDate, endDate);
        int scheduledWorkDays = 0;
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            MyAttendanceScheduleResolver.WorkSchedule schedule = scheduleResolver.resolve(policy, current);
            schedules.put(current, schedule);
            if (schedule.workday()) {
                scheduledWorkDays++;
            }
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime currentTime = LocalTime.now(clock);
        List<WeeklyEmployeeDetailView.Day> days = new java.util.ArrayList<>();
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            WeeklyEmployeeDetail record = records.get(current);
            MyAttendanceScheduleResolver.WorkSchedule schedule = schedules.get(current);
            MyAttendanceDayStatus status = resolveStatus(userId, current, schedule, record,
                    approvedLeaves.get(current), today, currentTime);
            days.add(new WeeklyEmployeeDetailView.Day(current, status,
                    record == null ? null : record.clockInAt(),
                    record == null ? null : record.clockOutAt()));
        }
        int attendedDays = (int) days.stream()
                .filter(day -> day.status() == MyAttendanceDayStatus.NORMAL
                        || day.status() == MyAttendanceDayStatus.LATE)
                .count();
        WeeklyEmployeeDetail employee = rows.get(0);
        WeeklyEmployeeDetailView result = new WeeklyEmployeeDetailView(
                new WeeklyEmployeeDetailView.Employee(employee.userId(), employee.name(), employee.roleName()),
                startDate, endDate, scheduledWorkDays, attendedDays, days);
        log.info("event=attendance_employee_weekly_detail_read_완료 userId={}, attendedDays={}", userId, attendedDays);
        return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_employee_weekly_detail_read_실패 requesterId={}, userId={}, errorType={}",
                    requesterId, userId, e.getClass().getSimpleName());
            throw e;
        }
    }

    private MyAttendanceDayStatus resolveStatus(
            Long userId, LocalDate date, MyAttendanceScheduleResolver.WorkSchedule schedule,
            WeeklyEmployeeDetail record, Set<Long> onLeave, LocalDate today, LocalTime currentTime) {
        if (!schedule.workday()) {
            return MyAttendanceDayStatus.OFF;
        }
        if (record != null && record.clockInAt() != null) {
            return MyAttendanceDayStatus.valueOf(record.attendanceStatus().name());
        }
        if (onLeave.contains(userId)) {
            return MyAttendanceDayStatus.LEAVE;
        }
        if (date.isBefore(today)
                || date.equals(today) && !currentTime.isBefore(schedule.startTime())) {
            return MyAttendanceDayStatus.ABSENT;
        }
        return MyAttendanceDayStatus.UNRECORDED;
    }
}
