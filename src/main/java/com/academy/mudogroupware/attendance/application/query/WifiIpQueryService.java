package com.academy.mudogroupware.attendance.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.GetWifiIpsUseCase;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WifiIpQueryService implements GetWifiIpsUseCase {

    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public List<AcademyWifiIp> getAll(Long requesterId) {
        log.info("event=attendance_wifi_ip_list_read_시작 requesterId={}", requesterId);
        try {
        List<AcademyWifiIp> result = academyWifiIpRepository.findAll();
        log.info("event=attendance_wifi_ip_list_read_완료 count={}", result.size());
        return result;
        } catch (RuntimeException e) {
            log.warn("event=attendance_wifi_ip_list_read_실패 requesterId={}, errorType={}",
                    requesterId, e.getClass().getSimpleName());
            throw e;
        }
    }
}
