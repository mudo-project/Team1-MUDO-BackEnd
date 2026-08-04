package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.Academy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

@ExtendWith(MockitoExtension.class)
class RegisterWifiIpServiceTest {

    @Mock
    private AcademyRepository academyRepository;

    @Mock
    private AcademyWifiIpRepository academyWifiIpRepository;

    @Test
    void checksDuplicateWithNormalizedIpAddress() {
        RegisterWifiIpService service = new RegisterWifiIpService(
                academyRepository, academyWifiIpRepository);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new Academy(1L, 10L)));
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "2001:db8:0:0:0:0:0:1"))
                .thenReturn(true);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.register(new RegisterWifiIpCommand(
                        10L, "2001:0db8:0000:0000:0000:0000:0000:0001", null)));

        assertSame(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED, exception.getErrorCode());
        verify(academyWifiIpRepository).existsByAcademyIdAndIpAddress(
                1L, "2001:db8:0:0:0:0:0:1");
    }
}
