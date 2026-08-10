package com.academy.mudogroupware.rollcall.infrastructure.external.aligo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record AligoProperties(String apiKey, String userId, String senderNumber, boolean testMode) {

    public AligoProperties(
            @Value("${ALIGO_API_KEY:}") String apiKey,
            @Value("${ALIGO_USER_ID:}") String userId,
            @Value("${ALIGO_SENDER_NUMBER:}") String senderNumber,
            @Value("${ALIGO_TEST_MODE:false}") boolean testMode) {
        this.apiKey = apiKey;
        this.userId = userId;
        this.senderNumber = senderNumber;
        this.testMode = testMode;
    }
}
