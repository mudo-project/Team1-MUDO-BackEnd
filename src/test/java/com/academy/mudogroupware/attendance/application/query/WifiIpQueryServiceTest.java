package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;
import com.academy.mudogroupware.attendance.domain.repository.AcademyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

class WifiIpQueryServiceTest {

    private final AcademyRepository academyRepository = mock(AcademyRepository.class);
    private final AcademyWifiIpRepository academyWifiIpRepository =
            mock(AcademyWifiIpRepository.class);
    private final WifiIpQueryService service =
            new WifiIpQueryService(academyRepository, academyWifiIpRepository);

    @Test
    void returnsWifiIpsRegisteredByRequesterAcademy() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 30);
        AcademyWifiIp wifiIp = AcademyWifiIp.restore(
                5L, 1L, "203.0.113.10", "본관 와이파이", createdAt, createdAt);
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(academyWifiIpRepository.findAllByAcademyId(1L))
                .thenReturn(List.of(wifiIp));

        List<AcademyWifiIp> result = service.getAll(10L);

        assertEquals(List.of(wifiIp), result);
        verify(academyWifiIpRepository).findAllByAcademyId(1L);
    }

    @Test
    void returnsEmptyListWhenRequesterAcademyHasNoWifiIp() {
        when(academyRepository.findByOwnerUserId(10L))
                .thenReturn(Optional.of(new OwnedAcademy(1L, 10L)));
        when(academyWifiIpRepository.findAllByAcademyId(1L))
                .thenReturn(List.of());

        assertEquals(List.of(), service.getAll(10L));
    }

    @Test
    void rejectsRequesterWhoDoesNotOwnAcademy() {
        when(academyRepository.findByOwnerUserId(10L)).thenReturn(Optional.empty());

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.getAll(10L));

        assertSame(AttendanceErrorCode.WIFI_IP_VIEW_FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(academyWifiIpRepository);
    }
}
