package com.academy.mudogroupware.attendance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;
import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;
import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegisterWifiIpService implements RegisterWifiIpUseCase {

    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public RegisterWifiIpResult register(RegisterWifiIpCommand command) {
        log.info("event=attendance_wifi_ip_register_시작 requesterId={}", command.requesterId());
        try {
        AcademyWifiIp confirmedWifiIp = AcademyWifiIp.create(
                command.confirmedIpAddress(), null);
        AcademyWifiIp detectedWifiIp = AcademyWifiIp.create(
                command.detectedIpAddress(), command.note());

        if (!confirmedWifiIp.getIpAddress().equals(detectedWifiIp.getIpAddress())) {
            throw new AttendanceException(AttendanceErrorCode.WIFI_IP_CHANGED);
        }

        if (academyWifiIpRepository.existsByIpAddress(
                detectedWifiIp.getIpAddress())) {
            throw new AttendanceException(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED);
        }

            RegisterWifiIpResult result = RegisterWifiIpResult.from(academyWifiIpRepository.save(detectedWifiIp));
            log.info("event=attendance_wifi_ip_register_완료 requesterId={}, wifiIpId={}",
                    command.requesterId(), result.wifiIpId());
            return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_wifi_ip_register_실패 requesterId={}, reason={}",
                    command.requesterId(), e.getMessage());
            throw e;
        }
    }
}
