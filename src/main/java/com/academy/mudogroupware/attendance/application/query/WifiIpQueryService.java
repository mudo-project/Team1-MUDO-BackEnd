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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WifiIpQueryService implements GetWifiIpsUseCase {

    private final AcademyRepository academyRepository;
    private final AcademyWifiIpRepository academyWifiIpRepository;

    @Override
    public List<AcademyWifiIp> getAll(Long requesterId) {
        OwnedAcademy academy = academyRepository.findByOwnerUserId(requesterId)
                .orElseThrow(() -> new AttendanceException(
                        AttendanceErrorCode.WIFI_IP_VIEW_FORBIDDEN));

        return academyWifiIpRepository.findAllByAcademyId(academy.id());
    }
}
