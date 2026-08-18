package com.academy.mudogroupware.sharedfile.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.application.usecase.RecreateSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// SHAREDFILE:ROOT_MANAGE 보유자가 명시적으로 요청하는 시스템 루트 재생성. AFTER_COMMIT 이벤트로
// 조용히 실패를 삼키는 SharedFileRootInitializer와 달리, 여기서는 Drive 호출 실패를 그대로 던져
// 요청자에게 알린다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecreateSharedFileRootService implements RecreateSharedFileRootUseCase {

    private static final String ROOT_FOLDER_NAME = "이음 그룹웨어 - 공유파일";

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;
    private final GetGoogleAccountConnectionUseCase getGoogleAccountConnectionUseCase;

    @Override
    public SharedFileRootView recreate() {
        Optional<SharedFileRoot> existing = sharedFileRootRepository.find();
        if (existing.isPresent() && existing.get().isReady()) {
            throw new IllegalStateException("이미 사용 가능한 시스템 루트가 존재합니다.");
        }

        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        String connectedGoogleEmail = getGoogleAccountConnectionUseCase.getConnection()
                .orElseThrow(() -> new IllegalStateException("Google 계정 연동 정보를 찾을 수 없습니다."))
                .googleEmail();
        DriveItem folder = sharedFileDrivePort.createRootFolder(accessToken, ROOT_FOLDER_NAME);

        SharedFileRoot root = existing.orElseGet(() -> SharedFileRoot.ready(folder.id(), connectedGoogleEmail));
        existing.ifPresent(r -> r.replaceWith(folder.id(), connectedGoogleEmail));
        save(root, accessToken, folder);

        return new SharedFileRootView(true, folder.id());
    }

    // DB 저장이 실패하면(동시 재생성 경합 등) 방금 만든 Drive 폴더가 고아로 남는다. 삭제(trash)로
    // 보상을 시도하고, 그마저 실패하면 로그로만 남긴 뒤 원래 저장 실패를 그대로 던진다 — 요청자가
    // 재생성을 다시 시도할 수 있게 성공으로 감추지 않는다.
    private void save(SharedFileRoot root, String accessToken, DriveItem folder) {
        try {
            sharedFileRootRepository.save(root);
        } catch (RuntimeException e) {
            log.warn("event=shared_file_root_recreate_save_failed folderId={} message={}",
                    folder.id(), e.getMessage());
            try {
                sharedFileDrivePort.trash(accessToken, folder.id());
            } catch (RuntimeException compensationFailure) {
                log.error("event=shared_file_root_recreate_compensation_failed folderId={} message={}",
                        folder.id(), compensationFailure.getMessage());
            }
            throw e;
        }
    }
}
