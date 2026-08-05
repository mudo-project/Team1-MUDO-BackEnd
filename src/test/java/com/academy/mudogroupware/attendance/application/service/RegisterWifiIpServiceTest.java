package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.command.RegisterWifiIpCommand;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
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
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "2001:db8:0:0:0:0:0:1"))
                .thenReturn(true);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.register(new RegisterWifiIpCommand(
                        10L,
                        "2001:0db8:0000:0000:0000:0000:0000:0001",
                        "2001:db8::1",
                        null)));

        assertSame(AttendanceErrorCode.WIFI_IP_ALREADY_REGISTERED, exception.getErrorCode());
        verify(academyWifiIpRepository).existsByAcademyIdAndIpAddress(
                1L, "2001:db8:0:0:0:0:0:1");
    }

    @Test
    void rejectsRegistrationWhenDetectedIpDiffersFromConfirmedIp() {
        RegisterWifiIpService service = new RegisterWifiIpService(
                academyRepository, academyWifiIpRepository);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.register(new RegisterWifiIpCommand(
                        10L, "203.0.113.10", "203.0.113.11", "본원")));

        assertSame(AttendanceErrorCode.WIFI_IP_CHANGED, exception.getErrorCode());
        verifyNoInteractions(academyWifiIpRepository);
    }

    @Test
    void savesAndReturnsNormalizedNote() {
        RegisterWifiIpService service = new RegisterWifiIpService(
                academyRepository, academyWifiIpRepository);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "203.0.113.10"))
                .thenReturn(false);
        when(academyWifiIpRepository.save(any(AcademyWifiIp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.register(new RegisterWifiIpCommand(
                10L, "203.0.113.10", "203.0.113.10", "  본원 와이파이  "));

        ArgumentCaptor<AcademyWifiIp> captor = ArgumentCaptor.forClass(AcademyWifiIp.class);
        verify(academyWifiIpRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getAcademyId());
        assertEquals("203.0.113.10", captor.getValue().getIpAddress());
        assertEquals("본원 와이파이", captor.getValue().getNote());
        assertEquals("203.0.113.10", result.ipAddress());
        assertEquals("본원 와이파이", result.note());
    }
}
