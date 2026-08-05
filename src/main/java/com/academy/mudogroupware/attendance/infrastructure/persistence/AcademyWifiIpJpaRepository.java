package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademyWifiIpJpaRepository extends JpaRepository<AcademyWifiIpJpaEntity, Long> {
    boolean existsByAcademyIdAndIpAddress(Long academyId, String ipAddress);

    List<AcademyWifiIpJpaEntity> findAllByAcademyIdOrderByCreatedAtAscIdAsc(Long academyId);

    long deleteByIdAndAcademyId(Long wifiIpId, Long academyId);
}
