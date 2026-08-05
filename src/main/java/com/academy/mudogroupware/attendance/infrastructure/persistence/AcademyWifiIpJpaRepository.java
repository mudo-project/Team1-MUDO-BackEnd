package com.academy.mudogroupware.attendance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademyWifiIpJpaRepository extends JpaRepository<AcademyWifiIpJpaEntity, Long> {
    boolean existsByAcademyIdAndIpAddress(Long academyId, String ipAddress);

    long deleteByIdAndAcademyId(Long wifiIpId, Long academyId);
}
