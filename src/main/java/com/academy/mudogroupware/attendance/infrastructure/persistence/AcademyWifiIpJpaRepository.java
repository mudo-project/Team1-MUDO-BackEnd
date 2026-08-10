package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AcademyWifiIpJpaRepository extends JpaRepository<AcademyWifiIpJpaEntity, Long> {
    boolean existsByIpAddress(String ipAddress);

    List<AcademyWifiIpJpaEntity> findAllByOrderByCreatedAtAscIdAsc();

    @Modifying
    @Query("delete from AcademyWifiIpJpaEntity w where w.id = :wifiIpId")
    int deleteByIdReturningCount(Long wifiIpId);
}
