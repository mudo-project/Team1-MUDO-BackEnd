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
        createOrRecreateRoot(existing);
    }

    // 기존 행이 있으면 그 객체를 그대로 바꿔서(replaceWith/markFailed) version을 유지한 채 저장한다.
    // find()로 읽은 뒤 완전히 새 SharedFileRoot.ready()/failed()를 만들어 버리면 version이 사라져,
    // 이미 있는 행인데도 SharedFileRootPersistenceAdapter가 insert를 시도해 PK 충돌로 실패한다.
    private void createOrRecreateRoot(Optional<SharedFileRoot> existing) {
        String accessToken;
        DriveItem folder;
        try {
            accessToken = getGoogleAccessTokenUseCase.getAccessToken();
            folder = sharedFileDrivePort.createRootFolder(accessToken, ROOT_FOLDER_NAME);
        } catch (RuntimeException e) {
            log.warn("event=shared_file_root_initialize_failed message={}", e.getMessage());
            persist(markFailed(existing));
            return;
        }

        SharedFileRoot result = markReady(existing, folder.id());
        persist(result, accessToken, folder.id());
    }

    private SharedFileRoot markReady(Optional<SharedFileRoot> existing, String googleRootFolderId) {
        return existing
                .map(root -> {
                    root.replaceWith(googleRootFolderId);
                    return root;
                })
                .orElseGet(() -> SharedFileRoot.ready(googleRootFolderId));
    }

    private SharedFileRoot markFailed(Optional<SharedFileRoot> existing) {
        return existing
                .map(root -> {
                    root.markFailed();
                    return root;
                })
                .orElseGet(SharedFileRoot::failed);
    }

    // Drive 호출 자체가 실패한 경우(폴더를 안 만들었으므로 보상할 대상이 없음) 저장만 시도한다.
    private void persist(SharedFileRoot result) {
        try {
            sharedFileRootRepository.save(result);
        } catch (DataAccessException e) {
            log.warn("event=shared_file_root_initialize_conflict message={}", e.getMessage());
        }
    }

    // 짧은 시간에 연결이 두 번 트리거되면(더블클릭 등) 두 스레드가 같은 행을 동시에 갱신할 수 있다.
    // SharedFileRootEntity의 @Version이 이를 감지해 나중 저장을 DataAccessException으로 실패시키는데,
    // 이때 "실패로 덮어쓰기"를 재시도하면 먼저 성공한 결과를 잘못 지울 수 있으므로 재시도하지 않는다.
    // 대신 이미 만들어버린 Drive 폴더가 고아로 남지 않도록 trash로 보상한다(RecreateSharedFileRootService와
    // 동일한 정책). 보상 자체가 실패해도 로그만 남기고 원래 저장 실패를 삼킨다 — AFTER_COMMIT이라 예외를
    // 던져도 받을 사람이 없다.
    private void persist(SharedFileRoot result, String accessToken, String folderId) {
        try {
            sharedFileRootRepository.save(result);
        } catch (DataAccessException e) {
            log.warn("event=shared_file_root_initialize_conflict folderId={} message={}", folderId, e.getMessage());
            try {
                sharedFileDrivePort.trash(accessToken, folderId);
            } catch (RuntimeException compensationFailure) {
                log.error("event=shared_file_root_initialize_compensation_failed folderId={} message={}",
                        folderId, compensationFailure.getMessage());
            }
        }
    }
}
