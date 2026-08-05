package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademyWifiIpRepositoryImpl implements AcademyWifiIpRepository {

    private static final String UNIQUE_CONSTRAINT_NAME = "uk_academy_wifi_ip";

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

        AcademyWifiIpJpaEntity saved;
        try {
            saved = academyWifiIpJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (isWifiIpUniqueConstraintViolation(e)) {
                throw new AttendanceException(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED);
            }
            throw e;
        }
        return AcademyWifiIp.restore(
                saved.getId(),
                saved.getAcademyId(),
                saved.getIpAddress(),
                saved.getNote(),
                saved.getCreatedAt(),
                saved.getUpdatedAt());
    }

    @Override
    public boolean deleteByIdAndAcademyId(Long wifiIpId, Long academyId) {
        return academyWifiIpJpaRepository.deleteByIdAndAcademyId(wifiIpId, academyId) > 0;
    }

    private boolean isWifiIpUniqueConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(UNIQUE_CONSTRAINT_NAME)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
