package com.academy.mudogroupware.rollcall.infrastructure.external.solapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.rollcall.application.port.SmsSendResult;

class SolapiSmsAdapterTest {

    private static final String SEND_URL = "https://api.solapi.com/messages/v4/send";

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("https://api.solapi.com");
    }

    private SolapiSmsAdapter adapter(RestClient.Builder builder, SolapiProperties properties) {
        return new SolapiSmsAdapter(builder.build(), properties);
    }

    @Test
    void returnsFailedResultWithoutCallingSolapiWhenPropertiesAreMissing() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        SmsSendResult result = adapter(builder, new SolapiProperties("", "", "010-1234-5678"))
                .send("010-1111-2222", "결석했습니다");

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("Solapi 설정");
        server.verify();
    }

    @Test
    void normalizesSenderAndReceiverPhoneNumbersWhenCallingSolapi() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", containsString("HMAC-SHA256 apiKey=test-key")))
                .andExpect(content().string(containsString("\"from\":\"01012345678\"")))
                .andExpect(content().string(containsString("\"to\":\"01011112222\"")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        SmsSendResult result = adapter(builder, new SolapiProperties("test-key", "test-secret", "010-1234-5678"))
                .send("010-1111-2222", "결석했습니다");

        assertThat(result.success()).isTrue();
        server.verify();
    }

    @Test
    void returnsFailedResultWhenSolapiRespondsWithAnErrorStatus() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SEND_URL)).andRespond(withServerError());

        SmsSendResult result = adapter(builder, new SolapiProperties("test-key", "test-secret", "010-1234-5678"))
                .send("010-1111-2222", "결석했습니다");

        assertThat(result.success()).isFalse();
        assertThat(result.indeterminate()).isFalse();
        server.verify();
    }

    @Test
    void returnsIndeterminateResultWhenConnectionFailsBeforeAResponseArrives() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SEND_URL)).andRespond(request -> {
            throw new IOException("Connection reset");
        });

        SmsSendResult result = adapter(builder, new SolapiProperties("test-key", "test-secret", "010-1234-5678"))
                .send("010-1111-2222", "결석했습니다");

        assertThat(result.success()).isFalse();
        assertThat(result.indeterminate()).isTrue();
        server.verify();
    }
}
