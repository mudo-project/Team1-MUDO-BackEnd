package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// GoogleDriveAdapter 전용 RestClient. Drive 업로드·다운로드는 OAuth 호출보다 오래 걸릴 수 있어
// GoogleOAuthConfig보다 읽기 타임아웃을 길게 잡는다.
@Configuration
public class GoogleDriveConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    @Bean
    RestClient googleDriveRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MILLIS));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
