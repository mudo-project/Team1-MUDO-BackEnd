package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;

class WifiIpQueryServiceTest {
    private final AcademyWifiIpRepository academyWifiIpRepository =
            mock(AcademyWifiIpRepository.class);
    private final WifiIpQueryService service =
            new WifiIpQueryService(academyWifiIpRepository);

    @Test
    void returnsWifiIpsRegisteredByRequesterAcademy() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 30);
        AcademyWifiIp wifiIp = AcademyWifiIp.restore(
                5L, "203.0.113.10", "본관 와이파이", createdAt, createdAt);
        when(academyWifiIpRepository.findAll())
                .thenReturn(List.of(wifiIp));

        List<AcademyWifiIp> result = service.getAll(10L);

        assertEquals(List.of(wifiIp), result);
        verify(academyWifiIpRepository).findAll();
    }

    @Test
    void returnsEmptyListWhenRequesterAcademyHasNoWifiIp() {
        when(academyWifiIpRepository.findAll())
                .thenReturn(List.of());

        assertEquals(List.of(), service.getAll(10L));
    }

}
