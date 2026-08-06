package com.academy.mudogroupware.google.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStatePort;
import com.academy.mudogroupware.google.domain.exception.GoogleOAuthStateInvalidException;

/**
 * OAuth state는 브라우저 리다이렉트를 거치므로 Authorization 헤더가 없다.
 * academyId·userId를 HMAC-SHA256으로 서명해 콜백에서 위조·재사용 없이 복원하기 위한 컴포넌트다.
 * 키는 JWT_SECRET을 재사용한다(별도 시크릿 설정 불필요).
 */
@Component
public class GoogleOAuthStateSigner implements GoogleOAuthStatePort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long STATE_VALID_SECONDS = 600;
    private static final String DELIMITER = ".";

    private final Clock clock;
    private final String jwtSecret;

    public GoogleOAuthStateSigner(Clock clock,
            @Value("${JWT_SECRET:local-development-only-change-this-secret-key}") String jwtSecret) {
        this.clock = clock;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public String sign(GoogleOAuthStateClaims claims) {
        long expiresAt = Instant.now(clock).getEpochSecond() + STATE_VALID_SECONDS;
        String payload = String.join(DELIMITER,
                String.valueOf(claims.academyId()),
                String.valueOf(claims.userId()),
                String.valueOf(claims.forceAccountSelection()),
                String.valueOf(expiresAt));
        return payload + DELIMITER + sign(payload);
    }

    @Override
    public GoogleOAuthStateClaims verify(String state) {
        if (state == null || state.isBlank()) {
            throw new GoogleOAuthStateInvalidException();
        }
        String[] parts = state.split("\\" + DELIMITER);
        if (parts.length != 5) {
            throw new GoogleOAuthStateInvalidException();
        }

        String payload = String.join(DELIMITER, parts[0], parts[1], parts[2], parts[3]);
        if (!sign(payload).equals(parts[4])) {
            throw new GoogleOAuthStateInvalidException();
        }

        long expiresAt = Long.parseLong(parts[3]);
        if (Instant.now(clock).getEpochSecond() > expiresAt) {
            throw new GoogleOAuthStateInvalidException();
        }

        return new GoogleOAuthStateClaims(Long.valueOf(parts[0]), Long.valueOf(parts[1]),
                Boolean.parseBoolean(parts[2]));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("state 서명에 실패했습니다.", e);
        }
    }
}
