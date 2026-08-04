package com.academy.mudogroupware.attendance.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AcademyWifiIpTest {

    @Test
    void normalizesEquivalentIpv6AddressesToSameValue() {
        AcademyWifiIp compressed = AcademyWifiIp.create(1L, "2001:db8::1", " 본원 ");
        AcademyWifiIp expanded = AcademyWifiIp.create(
                1L, "2001:0db8:0000:0000:0000:0000:0000:0001", "본원");

        assertEquals(compressed.getIpAddress(), expanded.getIpAddress());
        assertEquals("본원", compressed.getNote());
    }

    @Test
    void normalizesIpv4AndBlankNote() {
        AcademyWifiIp wifiIp = AcademyWifiIp.create(1L, "192.168.001.010", "   ");

        assertEquals("192.168.1.10", wifiIp.getIpAddress());
        assertNull(wifiIp.getNote());
    }
}
