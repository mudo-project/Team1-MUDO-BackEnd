package com.academy.mudogroupware.google.infrastructure.external.google;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleOAuthConfig {

    @Bean
    RestClient googleOAuthRestClient() {
        return RestClient.builder().build();
    }
}
