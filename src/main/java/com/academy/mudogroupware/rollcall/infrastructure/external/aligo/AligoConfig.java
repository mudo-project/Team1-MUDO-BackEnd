package com.academy.mudogroupware.rollcall.infrastructure.external.aligo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AligoConfig {

    @Bean
    RestClient aligoRestClient() {
        return RestClient.builder()
                .baseUrl("https://apis.aligo.in")
                .build();
    }
}
