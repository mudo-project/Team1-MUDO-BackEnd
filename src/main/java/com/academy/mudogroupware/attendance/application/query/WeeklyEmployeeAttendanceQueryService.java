package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.WeeklyAttendanceEmployee;
import com.academy.mudogroupware.attendance.application.port.WeeklyAttendanceQueryPort;
import com.academy.mudogroupware.attendance.application.usecase.GetWeeklyEmployeeAttendanceUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;
import com.academy.mudogroupware.global.domain.common.page.PagedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WeeklyEmployeeAttendanceQueryService implements GetWeeklyEmployeeAttendanceUseCase {

    private final AttendancePolicyRepository policyRepository;
    private final WeeklyAttendanceQueryPort attendanceQueryPort;
    private final LeaveRequestRepository leaveRequestRepository;
    private final MyAttendanceScheduleResolver scheduleResolver;
    private final Clock clock;

    @Override
    public WeeklyEmployeeAttendanceView getWeekly(
            Long requesterId, LocalDate date, String keyword,
            MyAttendanceDayStatus status, int page, int size) {
        log.info("event=attendance_employee_weekly_read_시작 requesterId={}, date={}, page={}, size={}", requesterId, date, page, size);
        try {
        if (date == null || page < 0 || size < 1 || size > 100) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_QUERY_PERIOD);
        }
        AttendancePolicy policy = policyRepository.findCurrent()
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));

        LocalDate startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = startDate.plusDays(6);
        LocalDate today = LocalDate.now(clock);
        LocalTimeSnapshot now = new LocalTimeSnapshot(today, java.time.LocalTime.now(clock));
        Map<Long, List<WeeklyAttendanceEmployee>> rows = attendanceQueryPort
                .findEmployees(requesterId, startDate, endDate)
                .stream().collect(Collectors.groupingBy(
                        WeeklyAttendanceEmployee::userId, LinkedHashMap::new, Collectors.toList()));

        int scheduledWorkDays = 0;
        Map<LocalDate, MyAttendanceScheduleResolver.WorkSchedule> schedules = new LinkedHashMap<>();
        Map<LocalDate, Set<Long>> approvedLeaves =
                leaveRequestRepository.findApprovedUserIdsBetween(startDate, endDate);
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            MyAttendanceScheduleResolver.WorkSchedule schedule = scheduleResolver.resolve(policy, current);
            boolean workday = schedule.workday();
            schedules.put(current, schedule);
            if (workday) {
                scheduledWorkDays++;
            }
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<WeeklyEmployeeAttendanceView.Employee> filtered = new ArrayList<>();
        for (Map.Entry<Long, List<WeeklyAttendanceEmployee>> entry : rows.entrySet()) {
            List<WeeklyAttendanceEmployee> employeeRows = entry.getValue();
            String name = employeeRows.get(0).name();
            String roleName = employeeRows.get(0).roleName();
            if (!normalizedKeyword.isEmpty() && !name.contains(normalizedKeyword)) {
                continue;
            }
            List<WeeklyEmployeeAttendanceView.Day> days = new ArrayList<>();
            for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
                LocalDate currentDate = current;
                WeeklyAttendanceEmployee record = employeeRows.stream()
                        .filter(row -> currentDate.equals(row.workDate()))
                        .findFirst().orElse(null);
                MyAttendanceDayStatus dayStatus = resolveStatus(
                        entry.getKey(), currentDate, schedules.get(currentDate), record,
                        approvedLeaves.get(currentDate), now);
                days.add(new WeeklyEmployeeAttendanceView.Day(
                        currentDate, dayStatus,
                        record == null ? null : record.clockInAt(),
                        record == null ? null : record.clockOutAt()));
            }
            if (status != null && days.stream().noneMatch(day -> day.status() == status)) {
                continue;
            }
            int attendedDays = (int) days.stream()
                    .filter(day -> day.status() == MyAttendanceDayStatus.NORMAL
                            || day.status() == MyAttendanceDayStatus.LATE)
                    .count();
            filtered.add(new WeeklyEmployeeAttendanceView.Employee(
                    entry.getKey(), name, roleName, attendedDays, scheduledWorkDays, days));
        }

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        WeeklyEmployeeAttendanceView result = new WeeklyEmployeeAttendanceView(startDate, endDate, scheduledWorkDays,
                PagedResult.of(filtered.subList(from, to), page, size, filtered.size()));
        log.info("event=attendance_employee_weekly_read_완료 count={}", filtered.size());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_employee_weekly_read_실패 requesterId={}, errorType={}",
                    requesterId, e.getClass().getSimpleName());
            throw e;
        }
    }

    private MyAttendanceDayStatus resolveStatus(Long userId, LocalDate date,
                                                 MyAttendanceScheduleResolver.WorkSchedule schedule,
                                                 WeeklyAttendanceEmployee record,
                                                 Set<Long> onLeave,
                                                 LocalTimeSnapshot now) {
        if (!schedule.workday()) {
            return MyAttendanceDayStatus.OFF;
        }
        if (record != null && record.clockInAt() != null) {
            return MyAttendanceDayStatus.valueOf(record.attendanceStatus().name());
        }
        if (onLeave.contains(userId)) {
            return MyAttendanceDayStatus.LEAVE;
        }
        if (date.isBefore(now.today())
                || date.equals(now.today()) && !now.currentTime().isBefore(schedule.startTime())) {
            return MyAttendanceDayStatus.ABSENT;
        }
        return MyAttendanceDayStatus.UNRECORDED;
    }

    private record LocalTimeSnapshot(LocalDate today, java.time.LocalTime currentTime) {
    }
}
