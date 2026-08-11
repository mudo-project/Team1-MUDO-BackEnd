package com.academy.mudogroupware.attendance.infrastructure.users;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;
import com.academy.mudogroupware.users.application.port.MemberTodayAttendanceStatus;
import com.academy.mudogroupware.users.application.port.TodayAttendanceStatusPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: users
 * Purpose: 구성원 목록 화면에 표시할 특정 사용자들의 오늘 근태 상태 조회
 */
@Component
@RequiredArgsConstructor
public class TodayAttendanceStatusAdapter implements TodayAttendanceStatusPort {

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final Clock clock;

    @Override
    public List<MemberTodayAttendanceStatus> findTodayStatusByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now(clock);
        AttendancePolicy policy = attendancePolicyRepository.findCurrent()
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));

        if (!policy.isWorkday(today.getDayOfWeek().getValue())) {
            return userIds.stream()
                    .map(userId -> new MemberTodayAttendanceStatus(userId, "OFF"))
                    .toList();
        }

        Set<Long> onLeaveUserIds = leaveRequestRepository.findApprovedUserIds(today);
        Map<Long, AttendanceRecord> recordByUserId = attendanceRecordRepository
                .findAllByUserIdsAndWorkDate(userIds, today).stream()
                .collect(Collectors.toMap(AttendanceRecord::getUserId, Function.identity()));

        return userIds.stream()
                .map(userId -> new MemberTodayAttendanceStatus(
                        userId, resolveStatus(userId, onLeaveUserIds, recordByUserId)))
                .toList();
    }

    private String resolveStatus(
            Long userId, Set<Long> onLeaveUserIds, Map<Long, AttendanceRecord> recordByUserId) {
        AttendanceRecord record = recordByUserId.get(userId);
        if (record != null && record.getClockInAt() != null) {
            return "PRESENT";
        }
        if (onLeaveUserIds.contains(userId)) {
            return "LEAVE";
        }
        return "ABSENT";
    }
}
