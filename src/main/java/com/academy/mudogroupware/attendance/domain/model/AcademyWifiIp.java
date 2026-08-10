package com.academy.mudogroupware.attendance.domain.model;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Arrays;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

public final class AcademyWifiIp {

    private final Long id;
    private final String ipAddress;
    private final String note;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AcademyWifiIp(Long id, String ipAddress, String note,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        String normalizedIpAddress = normalizeIpAddress(ipAddress);
        String normalizedNote = normalizeNote(note);
        if (normalizedNote != null && normalizedNote.length() > 100) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_WIFI_IP_NOTE);
        }
        this.id = id;
        this.ipAddress = normalizedIpAddress;
        this.note = normalizedNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AcademyWifiIp create(String ipAddress, String note) {
        LocalDateTime now = LocalDateTime.now();
        return new AcademyWifiIp(null, ipAddress, note, now, now);
    }

    public static AcademyWifiIp restore(Long id, String ipAddress, String note,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AcademyWifiIp(id, ipAddress, note, createdAt, updatedAt);
    }

    private static String normalizeIpAddress(String ipAddress) {
        if (isValidIpv4(ipAddress)) {
            return Arrays.stream(ipAddress.split("\\."))
                    .mapToInt(Integer::parseInt)
                    .mapToObj(Integer::toString)
                    .reduce((left, right) -> left + "." + right)
                    .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.INVALID_WIFI_IP));
        }
        if (isValidIpv6(ipAddress)) {
            try {
                return InetAddress.getByName(ipAddress).getHostAddress();
            } catch (UnknownHostException e) {
                throw new AttendanceException(AttendanceErrorCode.INVALID_WIFI_IP);
            }
        }
        throw new AttendanceException(AttendanceErrorCode.INVALID_WIFI_IP);
    }

    private static boolean isValidIpv4(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        String[] parts = ipAddress.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return false;
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIpv6(String ipAddress) {
        if (ipAddress == null || !ipAddress.contains(":")
                || !ipAddress.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getId() { return id; }
    public String getIpAddress() { return ipAddress; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
