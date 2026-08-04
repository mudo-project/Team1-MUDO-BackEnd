package com.academy.mudogroupware.attendance.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyWifiIpRepositoryImpl implements AcademyWifiIpRepository {

    private final AcademyWifiIpJpaRepository academyWifiIpJpaRepository;

    @Override
    public boolean existsByAcademyIdAndIpAddress(Long academyId, String ipAddress) {
        return academyWifiIpJpaRepository.existsByAcademyIdAndIpAddress(academyId, ipAddress);
    }

    @Override
    public AcademyWifiIp save(AcademyWifiIp wifiIp) {
        AcademyWifiIpJpaEntity entity = AcademyWifiIpJpaEntity.builder()
                .academyId(wifiIp.getAcademyId())
                .ipAddress(wifiIp.getIpAddress())
                .note(wifiIp.getNote())
                .createdAt(wifiIp.getCreatedAt())
                .updatedAt(wifiIp.getUpdatedAt())
                .build();

        AcademyWifiIpJpaEntity saved = academyWifiIpJpaRepository.save(entity);
        return AcademyWifiIp.restore(
                saved.getId(),
                saved.getAcademyId(),
                saved.getIpAddress(),
                saved.getNote(),
                saved.getCreatedAt(),
                saved.getUpdatedAt());
    }
}
