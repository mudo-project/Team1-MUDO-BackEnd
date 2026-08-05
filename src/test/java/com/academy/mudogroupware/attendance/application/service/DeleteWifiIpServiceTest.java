package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

@ExtendWith(MockitoExtension.class)
class DeleteWifiIpServiceTest {

    @Mock
    private AcademyRepository academyRepository;

    @Mock
    private AcademyWifiIpRepository academyWifiIpRepository;

    @Test
    void deletesWifiIpOwnedByRequesterAcademy() {
        DeleteWifiIpService service = new DeleteWifiIpService(
                academyRepository, academyWifiIpRepository);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(academyWifiIpRepository.deleteByIdAndAcademyId(5L, 1L))
                .thenReturn(true);

        service.delete(10L, 5L);

        verify(academyWifiIpRepository).deleteByIdAndAcademyId(5L, 1L);
    }

    @Test
    void rejectsDeletionWhenRequesterDoesNotOwnAcademy() {
        DeleteWifiIpService service = new DeleteWifiIpService(
                academyRepository, academyWifiIpRepository);
        when(academyRepository.findByOwnerUserId(10L)).thenReturn(Optional.empty());

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.delete(10L, 5L));

        assertSame(AttendanceErrorCode.WIFI_IP_DELETION_FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(academyWifiIpRepository);
    }

    @Test
    void rejectsDeletionWhenWifiIpDoesNotBelongToRequesterAcademy() {
        DeleteWifiIpService service = new DeleteWifiIpService(
                academyRepository, academyWifiIpRepository);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(academyWifiIpRepository.deleteByIdAndAcademyId(5L, 1L))
                .thenReturn(false);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.delete(10L, 5L));

        assertSame(AttendanceErrorCode.WIFI_IP_NOT_FOUND, exception.getErrorCode());
    }
}
