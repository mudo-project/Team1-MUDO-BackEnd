package com.academy.mudogroupware.attendance.domain.repository;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

public interface AcademyWifiIpRepository {
    boolean existsByAcademyIdAndIpAddress(Long academyId, String ipAddress);

    AcademyWifiIp save(AcademyWifiIp wifiIp);
}
