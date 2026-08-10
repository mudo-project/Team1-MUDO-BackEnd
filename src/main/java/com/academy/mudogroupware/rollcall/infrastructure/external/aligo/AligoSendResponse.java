package com.academy.mudogroupware.rollcall.infrastructure.external.aligo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 알리고 응답의 result_code는 성공 시 1, 실패 시 음수다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AligoSendResponse(
        @JsonProperty("result_code") int resultCode,
        String message) {

    public boolean isSuccess() {
        return resultCode == 1;
    }
}
