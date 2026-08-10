package com.academy.mudogroupware.users.application.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordSetupLinkBuilderTest {

    private final PasswordSetupLinkBuilder builder = new PasswordSetupLinkBuilder("http://localhost:3000");

    @Test
    void buildsLinkWithUsernameAndTempPasswordAsQueryParams() {
        String link = builder.build("teacher01", "Temp#Pass1");

        assertThat(link).isEqualTo("http://localhost:3000/password-setup?username=teacher01&tempPassword=Temp%23Pass1");
    }
}
