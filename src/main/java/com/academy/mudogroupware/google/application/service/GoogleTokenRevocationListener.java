package com.academy.mudogroupware.google.application.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.google.application.event.OldGoogleRefreshTokenRevocationRequestedEvent;
import com.academy.mudogroupware.google.application.port.GoogleOAuthPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GoogleTokenRevocationListener {

    private final GoogleOAuthPort googleOAuthPort;

    // DB 트랜잭션 커밋이 확정된 뒤에만 구글에 리프레시 토큰 폐기를 요청한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOldTokenRevocationRequested(OldGoogleRefreshTokenRevocationRequestedEvent event) {
        googleOAuthPort.revoke(event.oldRefreshToken());
    }
}
