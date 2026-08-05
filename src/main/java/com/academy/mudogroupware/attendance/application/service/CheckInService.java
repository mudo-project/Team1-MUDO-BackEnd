package com.academy.mudogroupware.attendance.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;
import com.academy.mudogroupware.attendance.application.result.CheckInResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckInUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckInService implements CheckInUseCase {

    private final AcademyWifiIpRepository academyWifiIpRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final Clock clock;

    @Override
    public CheckInResult checkIn(CheckInCommand command) {
        if (command.userId() == null || command.academyId() == null) {
            throw new AttendanceException(AttendanceErrorCode.CHECK_IN_FORBIDDEN);
        }

        AcademyWifiIp detectedWifiIp = AcademyWifiIp.create(
                command.academyId(), command.detectedIpAddress(), null);
        if (!academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                command.academyId(), detectedWifiIp.getIpAddress())) {
            throw new AttendanceException(AttendanceErrorCode.UNREGISTERED_CHECK_IN_IP);
        }

        AttendancePolicy policy = attendancePolicyRepository
                .findByAcademyId(command.academyId())
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        LocalTime workStartTime = resolveWorkStartTime(policy, now);

        if (attendanceRecordRepository.existsByAcademyIdAndUserIdAndWorkDate(
                command.academyId(), command.userId(), now.toLocalDate())) {
            throw new AttendanceException(
                    AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_IN);
        }

        AttendanceRecord record = AttendanceRecord.checkIn(
                command.academyId(), command.userId(), now, workStartTime,
                policy.getLateGraceMinutes(), command.clockInNote());
        return CheckInResult.from(attendanceRecordRepository.save(record));
    }

    private LocalTime resolveWorkStartTime(AttendancePolicy policy, LocalDateTime now) {
        if (!policy.isWeekdayExceptionEnabled()) {
            return policy.getDefaultStartTime();
        }
        Optional<AttendancePolicyWeekday> weekday = policy.getWeekdays().stream()
                .filter(item -> item.dayOfWeek() == now.getDayOfWeek().getValue())
                .findFirst();
        if (weekday.isEmpty()) {
            return policy.getDefaultStartTime();
        }
        AttendancePolicyWeekday setting = weekday.get();
        if (!setting.workday()) {
            throw new AttendanceException(AttendanceErrorCode.ATTENDANCE_NON_WORKDAY);
        }
        return setting.startTime() == null
                ? policy.getDefaultStartTime()
                : setting.startTime();
    }
}
