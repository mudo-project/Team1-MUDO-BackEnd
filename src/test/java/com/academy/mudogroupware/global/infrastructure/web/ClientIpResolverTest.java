package com.academy.mudogroupware.global.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;

import jakarta.servlet.http.HttpServletRequest;

class ClientIpResolverTest {

    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String SIGNING_SECRET = "test-client-ip-signing-secret";
    private static final String METHOD = "POST";
    private static final String PATH = "/api/attendance/check-ins";
    private static final String IP_ADDRESS = "203.0.113.10";

    @Test
    void resolvesRemoteAddressWhenNextProxyIsDisabled() {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, false, "", 60);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("::ffff:203.0.113.10");

        assertThat(resolver.resolve(request)).isEqualTo(IP_ADDRESS);
    }

    @Test
    void resolvesSignedClientIpFromNextProxy() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, true, SIGNING_SECRET, 60);
        String timestamp = String.valueOf(NOW.getEpochSecond());
        HttpServletRequest request = signedRequest(IP_ADDRESS, timestamp);

        assertThat(resolver.resolve(request)).isEqualTo(IP_ADDRESS);
    }

    @Test
    void resolvesSignedIpv6FromNextProxy() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, true, SIGNING_SECRET, 60);
        String timestamp = String.valueOf(NOW.getEpochSecond());
        String ipv6Address = "2001:db8::10";
        HttpServletRequest request = signedRequest(ipv6Address, timestamp);

        assertThat(resolver.resolve(request)).isEqualTo(ipv6Address);
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, true, SIGNING_SECRET, 60);
        String timestamp = String.valueOf(NOW.getEpochSecond());
        HttpServletRequest request = signedRequest(IP_ADDRESS, timestamp);
        when(request.getHeader("X-Client-IP-Signature")).thenReturn("invalid-signature");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsExpiredTimestamp() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, true, SIGNING_SECRET, 60);
        String timestamp = String.valueOf(NOW.minusSeconds(61).getEpochSecond());
        HttpServletRequest request = signedRequest(IP_ADDRESS, timestamp);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsMissingSignedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, true, SIGNING_SECRET, 60);
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsInvalidIpAddressEvenWithValidSignature() throws Exception {
        ClientIpResolver resolver = new ClientIpResolver(CLOCK, true, SIGNING_SECRET, 60);
        String timestamp = String.valueOf(NOW.getEpochSecond());
        HttpServletRequest request = signedRequest("not-an-ip", timestamp);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsMissingSigningSecretWhenNextProxyIsEnabled() {
        assertThatThrownBy(() -> new ClientIpResolver(CLOCK, true, " ", 60))
                .isInstanceOf(IllegalStateException.class);
    }

    private HttpServletRequest signedRequest(String ipAddress, String timestamp) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(METHOD);
        when(request.getRequestURI()).thenReturn(PATH);
        when(request.getHeader("X-Client-IP")).thenReturn(ipAddress);
        when(request.getHeader("X-Client-IP-Timestamp")).thenReturn(timestamp);
        when(request.getHeader("X-Client-IP-Signature"))
                .thenReturn(sign(METHOD, PATH, ipAddress, timestamp));
        return request;
    }

    private String sign(String method, String path, String ipAddress, String timestamp)
            throws Exception {
        String payload = String.join("\n", method, path, ipAddress, timestamp);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
