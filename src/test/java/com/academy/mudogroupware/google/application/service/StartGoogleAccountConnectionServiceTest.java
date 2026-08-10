package com.academy.mudogroupware.google.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.google.application.command.StartGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStateClaims;
import com.academy.mudogroupware.google.application.port.GoogleOAuthStatePort;

@ExtendWith(MockitoExtension.class)
class StartGoogleAccountConnectionServiceTest {

    @Mock private GoogleOAuthStatePort googleOAuthStatePort;
    @Mock private GoogleOAuthPort googleOAuthPort;

    private StartGoogleAccountConnectionService service;

    @BeforeEach
    void setUp() {
        service = new StartGoogleAccountConnectionService(googleOAuthStatePort, googleOAuthPort);
    }

    @Test
    void startSignsStateAndBuildsAuthorizationUrl() {
        StartGoogleConnectionCommand command = new StartGoogleConnectionCommand(7L, true);
        when(googleOAuthStatePort.sign(any())).thenReturn("signed-state");
        when(googleOAuthPort.buildAuthorizationUrl("signed-state", true)).thenReturn("https://accounts.google.com/auth");

        String authorizationUrl = service.start(command);

        assertThat(authorizationUrl).isEqualTo("https://accounts.google.com/auth");
        ArgumentCaptor<GoogleOAuthStateClaims> captor = ArgumentCaptor.forClass(GoogleOAuthStateClaims.class);
        verify(googleOAuthStatePort).sign(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new GoogleOAuthStateClaims(7L, true));
    }
}
