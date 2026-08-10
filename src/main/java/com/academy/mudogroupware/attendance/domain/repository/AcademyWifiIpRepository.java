package com.academy.mudogroupware.attendance.domain.repository;

import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

public interface AcademyWifiIpRepository {
    boolean existsByIpAddress(String ipAddress);

    AcademyWifiIp save(AcademyWifiIp wifiIp);

    List<AcademyWifiIp> findAll();

    boolean deleteById(Long wifiIpId);
}
