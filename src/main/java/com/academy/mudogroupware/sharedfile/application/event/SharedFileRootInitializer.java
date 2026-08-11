package com.academy.mudogroupware.sharedfile.application.event;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.google.application.event.GoogleAccountConnectedEvent;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Google 계정 연결 성공 이벤트를 커밋 이후 수신해 공유파일 시스템 루트를 자동 생성·갱신
@Slf4j
@Component
@RequiredArgsConstructor
public class SharedFileRootInitializer {

    private static final String ROOT_FOLDER_NAME = "이음 그룹웨어 - 공유파일";

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // 같은 계정으로 재연결했고 기존 루트가 READY면 그대로 두고, 그 외(최초 연결·계정 교체·FAILED 루트)는 재생성을 시도한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(GoogleAccountConnectedEvent event) {
        Optional<SharedFileRoot> existing = sharedFileRootRepository.find();
        if (existing.isPresent() && existing.get().isReady() && !event.accountChanged()) {
            return;
        }
        createOrRecreateRoot();
    }

    // Drive에 실제로 루트 폴더 생성을 시도한다. 실패해도 예외를 던지지 않고 FAILED로 저장해
    // Google 연결 자체(트랜잭션 커밋)가 이미 끝난 뒤이므로 연결 흐름에 영향을 주지 않는다.
    private void createOrRecreateRoot() {
        try {
            String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
            DriveItem folder = sharedFileDrivePort.createRootFolder(accessToken, ROOT_FOLDER_NAME);
            sharedFileRootRepository.save(SharedFileRoot.ready(folder.id()));
        } catch (RuntimeException e) {
            log.warn("event=shared_file_root_initialize_failed message={}", e.getMessage());
            sharedFileRootRepository.save(SharedFileRoot.failed());
        }
    }
}
