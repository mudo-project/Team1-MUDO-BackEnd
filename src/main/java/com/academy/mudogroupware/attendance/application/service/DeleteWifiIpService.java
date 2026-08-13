package com.academy.mudogroupware.attendance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.DeleteWifiIpUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeleteWifiIpService implements DeleteWifiIpUseCase {

    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public void delete(Long requesterId, Long wifiIpId) {
        log.info("event=attendance_wifi_ip_delete_시작 requesterId={}, wifiIpId={}", requesterId, wifiIpId);
        try {
            if (!academyWifiIpRepository.deleteById(wifiIpId)) {
                throw new AttendanceException(AttendanceErrorCode.WIFI_IP_NOT_FOUND);
            }
            log.info("event=attendance_wifi_ip_delete_완료 requesterId={}, wifiIpId={}", requesterId, wifiIpId);
        } catch (RuntimeException e) {
            log.warn("event=attendance_wifi_ip_delete_실패 requesterId={}, wifiIpId={}, errorType={}",
                    requesterId, wifiIpId, e.getClass().getSimpleName());
            throw e;
        }
    }
}
