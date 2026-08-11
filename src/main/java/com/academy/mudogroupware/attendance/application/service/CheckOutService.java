package com.academy.mudogroupware.attendance.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.application.result.CheckOutResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckOutUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CheckOutService implements CheckOutUseCase {

    private final AcademyWifiIpRepository academyWifiIpRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final Clock clock;

    @Override
    public CheckOutResult checkOut(CheckOutCommand command) {
        log.info("event=attendance_check_out_시작 userId={}={}", command.userId());
        try {
            if (command.userId() == null) {
                throw new AttendanceException(AttendanceErrorCode.CHECK_OUT_FORBIDDEN);
            }

        AcademyWifiIp detectedWifiIp = AcademyWifiIp.create(
                command.detectedIpAddress(), null);
        if (!academyWifiIpRepository.existsByIpAddress(
                detectedWifiIp.getIpAddress())) {
            throw new AttendanceException(AttendanceErrorCode.UNREGISTERED_CHECK_OUT_IP);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate earliestWorkDate = now.toLocalDate().minusDays(1);
        AttendanceRecord openRecord = attendanceRecordRepository.findLatestOpenSince(
                        command.userId(), earliestWorkDate)
                .orElseGet(() -> throwNoOpenRecord(command, now));

        AttendanceRecord checkedOut = openRecord.checkOut(
                now, command.clockOutType(), command.clockOutNote());
            CheckOutResult result = CheckOutResult.from(attendanceRecordRepository.save(checkedOut));
            log.info("event=attendance_check_out_완료 userId={}={}, clockOutType={}",
                    command.userId(), result.clockOutType());
            return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_check_out_실패 userId={}={}, reason={}",
                    command.userId(), e.getMessage());
            throw e;
        }
    }

    private AttendanceRecord throwNoOpenRecord(CheckOutCommand command, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        if (attendanceRecordRepository.existsCheckedOutBetween(
                command.userId(), startOfDay, now)) {
            throw new AttendanceException(AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT);
        }
        throw new AttendanceException(AttendanceErrorCode.ATTENDANCE_CHECK_IN_NOT_FOUND);
    }
}
