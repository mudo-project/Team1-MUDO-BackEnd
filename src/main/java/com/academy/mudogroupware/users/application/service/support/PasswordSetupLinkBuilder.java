package com.academy.mudogroupware.users.application.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PasswordSetupLinkBuilder {

    private final String frontendUrl;

    public PasswordSetupLinkBuilder(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String build(String username, String tempPassword) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/password-setup")
                .queryParam("username", username)
                .queryParam("tempPassword", tempPassword)
                .build()
                .encode()
                .toUriString();
    }
}
