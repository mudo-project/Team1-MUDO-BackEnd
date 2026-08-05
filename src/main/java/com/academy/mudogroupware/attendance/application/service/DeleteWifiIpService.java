package com.academy.mudogroupware.attendance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.DeleteWifiIpUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteWifiIpService implements DeleteWifiIpUseCase {

    private final AcademyRepository academyRepository;
    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public void delete(Long requesterId, Long wifiIpId) {
        OwnedAcademy academy = academyRepository.findByOwnerUserId(requesterId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.WIFI_IP_DELETION_FORBIDDEN));

        if (!academyWifiIpRepository.deleteByIdAndAcademyId(wifiIpId, academy.id())) {
            throw new AttendanceException(AttendanceErrorCode.WIFI_IP_NOT_FOUND);
        }
    }
}
