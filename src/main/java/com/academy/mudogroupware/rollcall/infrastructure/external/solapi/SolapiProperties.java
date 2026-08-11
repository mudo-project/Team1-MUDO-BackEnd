package com.academy.mudogroupware.rollcall.infrastructure.external.solapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record SolapiProperties(String apiKey, String apiSecret, String senderNumber) {

    public SolapiProperties(
            @Value("${SOLAPI_API_KEY:}") String apiKey,
            @Value("${SOLAPI_API_SECRET:}") String apiSecret,
            @Value("${SOLAPI_SENDER_NUMBER:}") String senderNumber) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.senderNumber = senderNumber;
    }
}
