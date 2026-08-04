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
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SaveAttendancePolicyService implements SaveAttendancePolicyUseCase {

    private final AcademyRepository academyRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;

    @Override
    public SaveAttendancePolicyResult save(SaveAttendancePolicyCommand command) {
        OwnedAcademy academy = academyRepository.findByOwnerUserId(command.requesterId())
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.ATTENDANCE_POLICY_SAVE_FORBIDDEN));

        AttendancePolicy policy = attendancePolicyRepository.findByAcademyId(academy.id())
                .map(existing -> existing.update(
                        command.defaultStartTime(), command.defaultEndTime(),
                        command.lateGraceMinutes(), command.weekdayExceptionEnabled(),
                        command.weekdays()))
                .orElseGet(() -> AttendancePolicy.create(
                        academy.id(), command.defaultStartTime(), command.defaultEndTime(),
                        command.lateGraceMinutes(), command.weekdayExceptionEnabled(),
                        command.weekdays() == null ? List.<AttendancePolicyWeekday>of()
                                : command.weekdays()));

        return SaveAttendancePolicyResult.from(attendancePolicyRepository.save(policy));
    }
}
