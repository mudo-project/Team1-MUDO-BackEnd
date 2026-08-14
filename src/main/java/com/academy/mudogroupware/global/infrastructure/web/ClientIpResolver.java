package com.academy.mudogroupware.global.infrastructure.web;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private static final String IPV4_MAPPED_IPV6_PREFIX = "::ffff:";
    private static final String CLIENT_IP_HEADER = "X-Client-IP";
    private static final String TIMESTAMP_HEADER = "X-Client-IP-Timestamp";
    private static final String SIGNATURE_HEADER = "X-Client-IP-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Clock clock;
    private final boolean nextProxyEnabled;
    private final byte[] signingSecret;
    private final Duration allowedClockSkew;

    public ClientIpResolver(
            Clock clock,
            @Value("${app.client-ip.next-proxy.enabled:false}") boolean nextProxyEnabled,
            @Value("${app.client-ip.next-proxy.signing-secret:}") String signingSecret,
            @Value("${app.client-ip.next-proxy.max-clock-skew-seconds:60}")
            long maxClockSkewSeconds) {
        if (nextProxyEnabled && !StringUtils.hasText(signingSecret)) {
            throw new IllegalStateException(
                    "CLIENT_IP_SIGNING_SECRET이 설정되지 않았습니다.");
        }
        if (maxClockSkewSeconds <= 0) {
            throw new IllegalStateException(
                    "CLIENT_IP_MAX_CLOCK_SKEW_SECONDS는 1 이상이어야 합니다.");
        }
        this.clock = clock;
        this.nextProxyEnabled = nextProxyEnabled;
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
        this.allowedClockSkew = Duration.ofSeconds(maxClockSkewSeconds);
    }

    public String resolve(HttpServletRequest request) {
        if (nextProxyEnabled) {
            return resolveFromNextProxy(request);
        }
        return normalizeIpv4MappedAddress(request.getRemoteAddr());
    }

    private String resolveFromNextProxy(HttpServletRequest request) {
        String ipAddress = header(request, CLIENT_IP_HEADER);
        String timestamp = header(request, TIMESTAMP_HEADER);
        String signature = header(request, SIGNATURE_HEADER);

        if (!validTimestamp(timestamp) || !validSignature(request, ipAddress, timestamp, signature)) {
            throw new ForbiddenException();
        }

        String normalizedIpAddress = normalizeIpv4MappedAddress(ipAddress);
        if (!isValidIpv4(normalizedIpAddress) && !isValidIpv6(normalizedIpAddress)) {
            throw new ForbiddenException();
        }
        return normalizedIpAddress;
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            throw new ForbiddenException();
        }
        return value.trim();
    }

    private boolean validTimestamp(String timestamp) {
        try {
            Instant requestedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            return Duration.between(requestedAt, clock.instant()).abs()
                    .compareTo(allowedClockSkew) <= 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean validSignature(
            HttpServletRequest request,
            String ipAddress,
            String timestamp,
            String signature) {
        String payload = String.join("\n",
                request.getMethod(), request.getRequestURI(), ipAddress, timestamp);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getUrlDecoder().decode(signature);
            return MessageDigest.isEqual(expected, actual);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidIpv4(String ipAddress) {
        String[] parts = ipAddress.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3
                    || !part.chars().allMatch(Character::isDigit)
                    || Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidIpv6(String ipAddress) {
        if (!ipAddress.contains(":") || !ipAddress.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            return InetAddress.getByName(ipAddress) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private String normalizeIpv4MappedAddress(String ipAddress) {
        if (ipAddress != null && ipAddress.startsWith(IPV4_MAPPED_IPV6_PREFIX)) {
            return ipAddress.substring(IPV4_MAPPED_IPV6_PREFIX.length());
        }
        return ipAddress;
    }
}
