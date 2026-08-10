package com.academy.mudogroupware.rollcall.infrastructure.external.aligo;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.rollcall.application.port.SmsSendResult;
import com.academy.mudogroupware.rollcall.application.port.SmsSenderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AligoSmsAdapter implements SmsSenderPort {

    private final RestClient aligoRestClient;
    private final AligoProperties aligoProperties;

    @Override
    public SmsSendResult send(String receiverPhone, String message) {
        String receiver = receiverPhone == null ? "" : receiverPhone.replaceAll("[^0-9]", "");
        if (receiver.isBlank()) {
            return SmsSendResult.failed("수신자 전화번호가 없습니다.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("key", aligoProperties.apiKey());
        body.add("user_id", aligoProperties.userId());
        body.add("sender", aligoProperties.senderNumber());
        body.add("receiver", receiver);
        body.add("msg", message);
        body.add("testmode_yn", aligoProperties.testMode() ? "Y" : "N");

        AligoSendResponse response;
        try {
            response = aligoRestClient.post()
                    .uri("/send/")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(AligoSendResponse.class);
        } catch (RestClientException e) {
            log.warn("event=aligo_sms_send_실패 receiver={}, reason={}", receiver, e.getMessage());
            return SmsSendResult.failed("SMS 발송 API 호출에 실패했습니다: " + e.getMessage());
        }

        if (response == null || !response.isSuccess()) {
            String reason = response != null ? response.message() : "응답이 비어 있습니다.";
            log.warn("event=aligo_sms_send_실패 receiver={}, reason={}", receiver, reason);
            return SmsSendResult.failed(reason);
        }
        return SmsSendResult.succeeded();
    }
}
