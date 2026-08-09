package com.academy.mudogroupware.attendance.application.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.GetWifiIpsUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WifiIpQueryService implements GetWifiIpsUseCase {

    private final AcademyRepository academyRepository;
    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public List<AcademyWifiIp> getAll(Long requesterId) {
        log.info("event=attendance_wifi_ip_list_read_시작 requesterId={}", requesterId);
        OwnedAcademy academy = academyRepository.findByOwnerUserId(requesterId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.WIFI_IP_VIEW_FORBIDDEN));

        List<AcademyWifiIp> result = academyWifiIpRepository.findAllByAcademyId(academy.id());
        log.info("event=attendance_wifi_ip_list_read_완료 academyId={}, count={}", academy.id(), result.size());
        return result;
    }
}
