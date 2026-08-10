package com.academy.mudogroupware.rollcall.infrastructure.external.solapi;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.rollcall.application.port.SmsSendResult;
import com.academy.mudogroupware.rollcall.application.port.SmsSenderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 인증: SOLAPI는 "HMAC-SHA256 apiKey=..., date=..., salt=..., signature=..." 형식의
// Authorization 헤더를 요구한다. signature = HMAC-SHA256(date+salt, key=apiSecret)의 16진수.
@Slf4j
@Component
@RequiredArgsConstructor
public class SolapiSmsAdapter implements SmsSenderPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RestClient solapiRestClient;
    private final SolapiProperties solapiProperties;

    @Override
    public SmsSendResult send(String receiverPhone, String message) {
        String receiver = receiverPhone == null ? "" : receiverPhone.replaceAll("[^0-9]", "");
        if (receiver.isBlank()) {
            return SmsSendResult.failed("수신자 전화번호가 없습니다.");
        }

        SolapiSendRequest body = new SolapiSendRequest(
                new SolapiMessageDto(solapiProperties.senderNumber(), receiver, message));

        String responseBody;
        try {
            responseBody = solapiRestClient.post()
                    .uri("/messages/v4/send")
                    .header("Authorization", authorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("event=solapi_sms_send_실패 receiver={}, reason={}", receiver, e.getMessage());
            return SmsSendResult.failed("SMS 발송 API 호출에 실패했습니다: " + e.getMessage());
        }

        log.info("event=solapi_sms_send_완료 receiver={}, response={}", receiver, responseBody);
        return SmsSendResult.succeeded();
    }

    private String authorizationHeader() {
        String date = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String signature = hmacSha256Hex(date + salt, solapiProperties.apiSecret());
        return "HMAC-SHA256 apiKey=" + solapiProperties.apiKey() + ", date=" + date + ", salt=" + salt
                + ", signature=" + signature;
    }

    private String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 생성에 실패했습니다.", e);
        }
    }
}
