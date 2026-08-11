package com.academy.mudogroupware.sharedfile.application.event;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
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

    // Drive 호출 결과를 계산하는 단계와 DB에 반영하는 단계를 분리한다 — 저장 단계에서 동시성 충돌이
    // 나더라도 "실패로 덮어쓰기"를 다시 시도하지 않기 위함이다(아래 persist() 주석 참고).
    private void createOrRecreateRoot() {
        SharedFileRoot result = resolveRoot();
        persist(result);
    }

    // Drive에 실제로 루트 폴더 생성을 시도한다. 실패해도 예외를 던지지 않고 FAILED로 계산해
    // Google 연결 자체(트랜잭션 커밋)가 이미 끝난 뒤이므로 연결 흐름에 영향을 주지 않는다.
    private SharedFileRoot resolveRoot() {
        try {
            String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
            DriveItem folder = sharedFileDrivePort.createRootFolder(accessToken, ROOT_FOLDER_NAME);
            return SharedFileRoot.ready(folder.id());
        } catch (RuntimeException e) {
            log.warn("event=shared_file_root_initialize_failed message={}", e.getMessage());
            return SharedFileRoot.failed();
        }
    }

    // 짧은 시간에 연결이 두 번 트리거되면(더블클릭 등) 두 스레드가 같은 행을 동시에 갱신할 수 있다.
    // SharedFileRootEntity의 @Version이 이를 감지해 나중 저장을 DataAccessException으로 실패시키는데,
    // 이때 "실패로 덮어쓰기"를 재시도하면 먼저 성공한 결과를 잘못 지울 수 있으므로 그냥 포기하고 로그만 남긴다.
    private void persist(SharedFileRoot result) {
        try {
            sharedFileRootRepository.save(result);
        } catch (DataAccessException e) {
            log.warn("event=shared_file_root_initialize_conflict message={}", e.getMessage());
        }
    }
}
