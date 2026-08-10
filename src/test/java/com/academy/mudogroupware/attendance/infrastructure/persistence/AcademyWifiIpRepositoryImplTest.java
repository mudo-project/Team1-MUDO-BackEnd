package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;

class AcademyWifiIpRepositoryImplTest {

    @Test
    void findsWifiIpsWithinAcademyScopeInRegistrationOrder() {
        AcademyWifiIpJpaRepository jpaRepository = mock(AcademyWifiIpJpaRepository.class);
        AcademyWifiIpRepositoryImpl repository = new AcademyWifiIpRepositoryImpl(jpaRepository);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 30);
        AcademyWifiIpJpaEntity entity = AcademyWifiIpJpaEntity.builder()
                .ipAddress("203.0.113.10")
                .note("본관 와이파이")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        when(jpaRepository.findAllByOrderByCreatedAtAscIdAsc())
                .thenReturn(List.of(entity));

        List<AcademyWifiIp> result = repository.findAll();

        assertEquals(1, result.size());
        assertNull(result.get(0).getId());
        assertEquals("203.0.113.10", result.get(0).getIpAddress());
        verify(jpaRepository).findAllByOrderByCreatedAtAscIdAsc();
    }

    @Test
    void deletesWifiIpWithinAcademyScope() {
        AcademyWifiIpJpaRepository jpaRepository =
                org.mockito.Mockito.mock(AcademyWifiIpJpaRepository.class);
        AcademyWifiIpRepositoryImpl repository = new AcademyWifiIpRepositoryImpl(jpaRepository);
        when(jpaRepository.deleteByIdReturningCount(5L)).thenReturn(1);

        boolean deleted = repository.deleteById(5L);

        assertTrue(deleted);
        verify(jpaRepository).deleteByIdReturningCount(5L);
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
                () -> repository.save(AcademyWifiIp.create( "203.0.113.10", null)));

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
                () -> repository.save(AcademyWifiIp.create( "203.0.113.10", null)));

        assertSame(violation, thrown);
    }
}
