package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.EmploymentSummaryPort;
import com.academy.mudogroupware.attendance.application.usecase.GetMyMonthlyAttendanceUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyMonthlyAttendanceQueryService implements GetMyMonthlyAttendanceUseCase {

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmploymentSummaryPort employmentSummaryPort;
    private final MyAttendanceScheduleResolver scheduleResolver;
    private final MyAttendanceStatusResolver statusResolver;
    private final Clock clock;

    @Override
    public MyMonthlyAttendanceView getMonthly(
            Long userId, Long academyId, int year, int month) {
        log.info("event=attendance_monthly_read_시작 userId={}, academyId={}, year={}, month={}", userId, academyId, year, month);
        YearMonth targetMonth = toYearMonth(year, month);
        LocalDate today = LocalDate.now(clock);
        LocalDate hireDate = employmentSummaryPort.findByUserIdAndAcademyId(userId, academyId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.EMPLOYMENT_INFO_NOT_FOUND))
                .hireDate();
        LocalDate startDate = laterOf(targetMonth.atDay(1), hireDate);
        LocalDate endDate = earlierOf(targetMonth.atEndOfMonth(), today);
        if (startDate.isAfter(endDate)) {
            MyMonthlyAttendanceView result = new MyMonthlyAttendanceView(year, month, List.of());
            log.info("event=attendance_monthly_read_완료 userId={}, academyId={}, count=0", userId, academyId); return result;
        }

        AttendancePolicy policy = attendancePolicyRepository.findByAcademyId(academyId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));
        Map<LocalDate, AttendanceRecord> records = attendanceRecordRepository
                .findByAcademyIdAndUserIdAndWorkDateBetween(
                        academyId, userId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getWorkDate, Function.identity()));
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedOverlapping(
                academyId, userId, startDate, endDate);
        LocalTime currentTime = LocalTime.now(clock);

        List<MyMonthlyAttendanceView.Day> days = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> toDay(
                        date, policy, records.get(date), approvedLeaves, today, currentTime))
                .toList();
        MyMonthlyAttendanceView result = new MyMonthlyAttendanceView(year, month, days);
        log.info("event=attendance_monthly_read_완료 userId={}, academyId={}, count={}", userId, academyId, days.size());
        return result;
    }

    private MyMonthlyAttendanceView.Day toDay(
            LocalDate date, AttendancePolicy policy, AttendanceRecord record,
            List<LeaveRequest> approvedLeaves, LocalDate today, LocalTime currentTime) {
        var schedule = scheduleResolver.resolve(policy, date);
        var status = statusResolver.resolve(
                date, schedule, record, approvedLeaves, today, currentTime);
        return new MyMonthlyAttendanceView.Day(
                date,
                status,
                record == null ? null : record.getClockInAt().toLocalTime(),
                record == null || record.getClockOutAt() == null
                        ? null : record.getClockOutAt().toLocalTime());
    }

    private YearMonth toYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new AttendanceException(
                    AttendanceErrorCode.INVALID_ATTENDANCE_QUERY_PERIOD, exception);
        }
    }

    private LocalDate laterOf(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDate earlierOf(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }
}
