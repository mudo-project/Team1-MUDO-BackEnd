package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.UploadSharedFileUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidNameException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileUploadTooLargeException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadSharedFileService implements UploadSharedFileUseCase {

    private static final int MAX_UPLOAD_BYTES = 100 * 1024 * 1024;

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // 호출자가 별도로 선언한 크기 값을 신뢰하지 않고, 실제 전송된 content 길이로만 100MB를 검사한다
    // (선언 크기를 실제보다 작게 보내는 방식으로는 제한을 우회할 수 없다).
    // parentId를 생략하면(null) 시스템 루트 바로 아래에 업로드한다 — ListSharedFileItemsService의 목록 조회와
    // 동일한 규칙이다.
    @Override
    public SharedFileItemView upload(String parentId, String filename, String contentType, byte[] content) {
        if (content.length > MAX_UPLOAD_BYTES) {
            throw new SharedFileUploadTooLargeException();
        }
        if (filename == null || filename.isBlank()) {
            throw new SharedFileInvalidNameException();
        }
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();

        String rootId = root.getGoogleRootFolderId();
        String targetParentId = parentId == null ? rootId : parentId;
        if (!targetParentId.equals(rootId)) {
            sharedFileRootGuard.requireDescendant(accessToken, rootId, targetParentId);
        }

        DriveItem uploaded = sharedFileDrivePort.upload(accessToken, targetParentId, filename, contentType, content);
        return SharedFileItemViewMapper.toView(uploaded);
    }
}
