package com.academy.mudogroupware.google.application.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;

@ExtendWith(MockitoExtension.class)
class GoogleTokenRevocationListenerTest {

    @Mock private GoogleOAuthPort googleOAuthPort;

    @Test
    void onOldTokenRevocationRequestedCallsRevokeWithTheGivenToken() {
        GoogleTokenRevocationListener listener = new GoogleTokenRevocationListener(googleOAuthPort);

        listener.onOldTokenRevocationRequested(new OldGoogleRefreshTokenRevocationRequestedEvent("old-refresh-token"));

        verify(googleOAuthPort).revoke("old-refresh-token");
    }
}
