package com.academy.mudogroupware.attendance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;
import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;
import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
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
        OwnedAcademy academy = academyRepository.findByOwnerUserId(command.requesterId())
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.WIFI_IP_REGISTRATION_FORBIDDEN));

        AcademyWifiIp confirmedWifiIp = AcademyWifiIp.create(
                academy.id(), command.confirmedIpAddress(), null);
        AcademyWifiIp detectedWifiIp = AcademyWifiIp.create(
                academy.id(), command.detectedIpAddress(), command.note());

        if (!confirmedWifiIp.getIpAddress().equals(detectedWifiIp.getIpAddress())) {
            throw new AttendanceException(AttendanceErrorCode.WIFI_IP_CHANGED);
        }

        if (academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                academy.id(), detectedWifiIp.getIpAddress())) {
            throw new AttendanceException(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED);
        }

        return RegisterWifiIpResult.from(academyWifiIpRepository.save(detectedWifiIp));
    }
}
