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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeleteWifiIpService implements DeleteWifiIpUseCase {

    private final AcademyRepository academyRepository;
    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public void delete(Long requesterId, Long wifiIpId) {
        log.info("event=attendance_wifi_ip_delete_시작 requesterId={}, wifiIpId={}", requesterId, wifiIpId);
        try {
            OwnedAcademy academy = academyRepository.findByOwnerUserId(requesterId)
                    .orElseThrow(() -> new AttendanceException(
                            AttendanceErrorCode.WIFI_IP_DELETION_FORBIDDEN));

            if (!academyWifiIpRepository.deleteByIdAndAcademyId(wifiIpId, academy.id())) {
                throw new AttendanceException(AttendanceErrorCode.WIFI_IP_NOT_FOUND);
            }
            log.info("event=attendance_wifi_ip_delete_완료 requesterId={}, wifiIpId={}", requesterId, wifiIpId);
        } catch (RuntimeException e) {
            log.warn("event=attendance_wifi_ip_delete_실패 requesterId={}, wifiIpId={}, reason={}",
                    requesterId, wifiIpId, e.getMessage());
            throw e;
        }
    }
}
