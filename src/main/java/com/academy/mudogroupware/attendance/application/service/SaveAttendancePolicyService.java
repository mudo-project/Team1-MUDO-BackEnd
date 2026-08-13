package com.academy.mudogroupware.attendance.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.SaveAttendancePolicyCommand;
import com.academy.mudogroupware.attendance.application.result.SaveAttendancePolicyResult;
import com.academy.mudogroupware.attendance.application.usecase.SaveAttendancePolicyUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SaveAttendancePolicyService implements SaveAttendancePolicyUseCase {

    private final AttendancePolicyRepository attendancePolicyRepository;

    @Override
    public SaveAttendancePolicyResult save(SaveAttendancePolicyCommand command) {
        log.info("event=attendance_policy_save_시작 requesterId={}", command.requesterId());
        try {
        AttendancePolicy policy = attendancePolicyRepository.findCurrent()
                .map(existing -> existing.update(
                        command.defaultStartTime(), command.defaultEndTime(),
                        command.lateGraceMinutes(), command.weekdayExceptionEnabled(),
                        command.weekdays()))
                .orElseGet(() -> AttendancePolicy.create(
                        command.defaultStartTime(), command.defaultEndTime(),
                        command.lateGraceMinutes(), command.weekdayExceptionEnabled(),
                        command.weekdays() == null ? List.<AttendancePolicyWeekday>of()
                                : command.weekdays()));

            SaveAttendancePolicyResult result = SaveAttendancePolicyResult.from(attendancePolicyRepository.save(policy));
            log.info("event=attendance_policy_save_완료 requesterId={}, policyId={}",
                    command.requesterId(), result.policyId());
            return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_policy_save_실패 requesterId={}, errorType={}",
                    command.requesterId(), e.getClass().getSimpleName());
            throw e;
        }
    }
}
