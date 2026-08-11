package com.academy.mudogroupware.sharedfile.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.application.usecase.RecreateSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

// SHAREDFILE:ROOT_MANAGE 보유자가 명시적으로 요청하는 시스템 루트 재생성. AFTER_COMMIT 이벤트로
// 조용히 실패를 삼키는 SharedFileRootInitializer와 달리, 여기서는 Drive 호출 실패를 그대로 던져
// 요청자에게 알린다.
@Service
@RequiredArgsConstructor
@Transactional
public class RecreateSharedFileRootService implements RecreateSharedFileRootUseCase {

    private static final String ROOT_FOLDER_NAME = "이음 그룹웨어 - 공유파일";

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    @Override
    public SharedFileRootView recreate() {
        Optional<SharedFileRoot> existing = sharedFileRootRepository.find();
        if (existing.isPresent() && existing.get().isReady()) {
            throw new IllegalStateException("이미 사용 가능한 시스템 루트가 존재합니다.");
        }

        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        DriveItem folder = sharedFileDrivePort.createRootFolder(accessToken, ROOT_FOLDER_NAME);

        SharedFileRoot root = existing.orElseGet(() -> SharedFileRoot.ready(folder.id()));
        existing.ifPresent(r -> r.replaceWith(folder.id()));
        sharedFileRootRepository.save(root);

        return new SharedFileRootView(true);
    }
}
