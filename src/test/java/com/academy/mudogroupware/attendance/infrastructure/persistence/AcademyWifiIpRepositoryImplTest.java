package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

class AcademyWifiIpRepositoryImplTest {

    @Test
    void deletesWifiIpWithinAcademyScope() {
        AcademyWifiIpJpaRepository jpaRepository =
                org.mockito.Mockito.mock(AcademyWifiIpJpaRepository.class);
        AcademyWifiIpRepositoryImpl repository = new AcademyWifiIpRepositoryImpl(jpaRepository);
        when(jpaRepository.deleteByIdAndAcademyId(5L, 1L)).thenReturn(1L);

        boolean deleted = repository.deleteByIdAndAcademyId(5L, 1L);

        assertTrue(deleted);
        verify(jpaRepository).deleteByIdAndAcademyId(5L, 1L);
    }

    @Test
    void convertsWifiIpUniqueConstraintViolationToDomainException() {
        AcademyWifiIpJpaRepository jpaRepository =
                org.mockito.Mockito.mock(AcademyWifiIpJpaRepository.class);
        AcademyWifiIpRepositoryImpl repository = new AcademyWifiIpRepositoryImpl(jpaRepository);
        when(jpaRepository.saveAndFlush(any(AcademyWifiIpJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_academy_wifi_ip'"));

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> repository.save(AcademyWifiIp.create(1L, "203.0.113.10", null)));

        assertSame(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED, exception.getErrorCode());
    }

    @Test
    void preservesUnrelatedDataIntegrityViolation() {
        AcademyWifiIpJpaRepository jpaRepository =
                org.mockito.Mockito.mock(AcademyWifiIpJpaRepository.class);
        AcademyWifiIpRepositoryImpl repository = new AcademyWifiIpRepositoryImpl(jpaRepository);
        DataIntegrityViolationException violation =
                new DataIntegrityViolationException("foreign key violation");
        when(jpaRepository.saveAndFlush(any(AcademyWifiIpJpaEntity.class)))
                .thenThrow(violation);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(AcademyWifiIp.create(1L, "203.0.113.10", null)));

        assertSame(violation, thrown);
    }
}
