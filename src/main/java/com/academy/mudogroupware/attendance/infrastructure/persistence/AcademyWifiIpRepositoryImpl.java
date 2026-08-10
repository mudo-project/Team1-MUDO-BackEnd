package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.Locale;
import java.util.List;

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
    public boolean existsByIpAddress(String ipAddress) {
        return academyWifiIpJpaRepository.existsByIpAddress(ipAddress);
    }

    @Override
    public AcademyWifiIp save(AcademyWifiIp wifiIp) {
        AcademyWifiIpJpaEntity entity = AcademyWifiIpJpaEntity.builder()
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
        return toDomain(saved);
    }

    @Override
    public List<AcademyWifiIp> findAll() {
        return academyWifiIpJpaRepository
                .findAllByOrderByCreatedAtAscIdAsc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean deleteById(Long wifiIpId) {
        return academyWifiIpJpaRepository.deleteByIdReturningCount(wifiIpId) > 0;
    }

    private AcademyWifiIp toDomain(AcademyWifiIpJpaEntity entity) {
        return AcademyWifiIp.restore(
                entity.getId(),
                entity.getIpAddress(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
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
