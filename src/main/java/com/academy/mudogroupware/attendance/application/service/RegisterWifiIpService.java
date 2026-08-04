package com.academy.mudogroupware.attendance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;
import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;
import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.Academy;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterWifiIpService implements RegisterWifiIpUseCase {

    private final AcademyRepository academyRepository;
    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public RegisterWifiIpResult register(RegisterWifiIpCommand command) {
        Academy academy = academyRepository.findByOwnerUserId(command.requesterId())
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.WIFI_IP_REGISTRATION_FORBIDDEN));

        if (academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                academy.id(), command.ipAddress())) {
            throw new AttendanceException(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED);
        }

        AcademyWifiIp wifiIp = AcademyWifiIp.create(
                academy.id(), command.ipAddress(), command.note());

        return RegisterWifiIpResult.from(academyWifiIpRepository.save(wifiIp));
    }
}
