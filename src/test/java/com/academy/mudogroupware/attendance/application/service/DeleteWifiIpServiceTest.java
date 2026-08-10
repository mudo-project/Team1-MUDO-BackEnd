package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

@ExtendWith(MockitoExtension.class)
class DeleteWifiIpServiceTest {

    @Mock
    private AcademyWifiIpRepository academyWifiIpRepository;

    @Test
    void deletesWifiIpOwnedByRequesterAcademy() {
        DeleteWifiIpService service = new DeleteWifiIpService(
                academyWifiIpRepository);
        when(academyWifiIpRepository.deleteById(5L))
                .thenReturn(true);

        service.delete(10L, 5L);

        verify(academyWifiIpRepository).deleteById(5L);
    }

    @Test
    void rejectsDeletionWhenWifiIpDoesNotBelongToRequesterAcademy() {
        DeleteWifiIpService service = new DeleteWifiIpService(
                academyWifiIpRepository);
        when(academyWifiIpRepository.deleteById(5L))
                .thenReturn(false);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.delete(10L, 5L));

        assertSame(AttendanceErrorCode.WIFI_IP_NOT_FOUND, exception.getErrorCode());
    }
}
