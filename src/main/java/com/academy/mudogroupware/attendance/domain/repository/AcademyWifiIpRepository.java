package com.academy.mudogroupware.attendance.domain.repository;

import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

public interface AcademyWifiIpRepository {
    boolean existsByAcademyIdAndIpAddress(Long academyId, String ipAddress);

    AcademyWifiIp save(AcademyWifiIp wifiIp);

    List<AcademyWifiIp> findAllByAcademyId(Long academyId);

    boolean deleteByIdAndAcademyId(Long wifiIpId, Long academyId);
}
