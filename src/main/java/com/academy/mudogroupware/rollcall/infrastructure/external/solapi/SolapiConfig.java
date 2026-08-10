package com.academy.mudogroupware.rollcall.infrastructure.external.solapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SolapiConfig {

    @Bean
    RestClient solapiRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.solapi.com")
                .build();
    }
}
