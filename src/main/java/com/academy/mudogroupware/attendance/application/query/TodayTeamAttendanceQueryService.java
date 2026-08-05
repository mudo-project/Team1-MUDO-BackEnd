package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.TeamAttendanceEmployee;
import com.academy.mudogroupware.attendance.application.port.TeamAttendanceQueryPort;
import com.academy.mudogroupware.attendance.application.usecase.GetTodayTeamAttendanceUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.model.TeamAttendanceStatus;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayTeamAttendanceQueryService implements GetTodayTeamAttendanceUseCase {

    private static final String[] KOREAN_DAY_NAMES = {
            "월", "화", "수", "목", "금", "토", "일"
    };

    private final AcademyRepository academyRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final TeamAttendanceQueryPort teamAttendanceQueryPort;
    private final Clock clock;

    @Override
    public TodayTeamAttendanceView getToday(Long requesterId, Long academyId) {
        OwnedAcademy academy = academyRepository.findByOwnerUserId(requesterId)
                .filter(owned -> owned.id().equals(academyId))
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.TEAM_ATTENDANCE_VIEW_FORBIDDEN));
        AttendancePolicy policy = attendancePolicyRepository.findByAcademyId(academy.id())
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));

        LocalDate today = LocalDate.now(clock);
        WorkSchedule schedule = resolveSchedule(policy, today);
        List<TodayTeamAttendanceView.Employee> employees = teamAttendanceQueryPort
                .findEmployeesWithAttendance(academy.id(), academy.ownerUserId(), today)
                .stream()
                .map(employee -> toEmployee(employee, schedule.workday()))
                .toList();

        int presentCount = (int) employees.stream()
                .filter(employee -> employee.status() == TeamAttendanceStatus.PRESENT)
                .count();
        int absentCount = (int) employees.stream()
                .filter(employee -> employee.status() == TeamAttendanceStatus.ABSENT)
                .count();
        int offCount = (int) employees.stream()
                .filter(employee -> employee.status() == TeamAttendanceStatus.OFF)
                .count();

        return new TodayTeamAttendanceView(
                today,
                KOREAN_DAY_NAMES[today.getDayOfWeek().getValue() - 1],
                schedule.startTime(),
                schedule.endTime(),
                new TodayTeamAttendanceView.Summary(presentCount, absentCount, offCount),
                employees);
    }

    private TodayTeamAttendanceView.Employee toEmployee(
            TeamAttendanceEmployee employee, boolean workday) {
        TeamAttendanceStatus status = !workday
                ? TeamAttendanceStatus.OFF
                : employee.clockInAt() == null
                        ? TeamAttendanceStatus.ABSENT
                        : TeamAttendanceStatus.PRESENT;
        LocalTime checkInTime = status == TeamAttendanceStatus.PRESENT
                ? employee.clockInAt().toLocalTime()
                : null;
        return new TodayTeamAttendanceView.Employee(
                employee.userId(), employee.name(), status, checkInTime);
    }

    private WorkSchedule resolveSchedule(AttendancePolicy policy, LocalDate today) {
        if (!policy.isWeekdayExceptionEnabled()) {
            return new WorkSchedule(
                    true, policy.getDefaultStartTime(), policy.getDefaultEndTime());
        }

        Optional<AttendancePolicyWeekday> weekday = policy.getWeekdays().stream()
                .filter(item -> item.dayOfWeek() == today.getDayOfWeek().getValue())
                .findFirst();
        if (weekday.isEmpty()) {
            return new WorkSchedule(
                    true, policy.getDefaultStartTime(), policy.getDefaultEndTime());
        }

        AttendancePolicyWeekday setting = weekday.get();
        if (!setting.workday()) {
            return new WorkSchedule(
                    false, policy.getDefaultStartTime(), policy.getDefaultEndTime());
        }
        return new WorkSchedule(
                true,
                setting.startTime() == null ? policy.getDefaultStartTime() : setting.startTime(),
                setting.endTime() == null ? policy.getDefaultEndTime() : setting.endTime());
    }

    private record WorkSchedule(boolean workday, LocalTime startTime, LocalTime endTime) {
    }
}
