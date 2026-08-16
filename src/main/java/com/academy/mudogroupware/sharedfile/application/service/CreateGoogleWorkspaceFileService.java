package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.CreateGoogleWorkspaceFileUseCase;
import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidNameException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateGoogleWorkspaceFileService implements CreateGoogleWorkspaceFileUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // parentId를 생략하면(null) 시스템 루트 바로 아래에 만든다 — ListSharedFileItemsService의 목록 조회와
    // 동일한 규칙이다.
    @Override
    public SharedFileItemView create(String parentId, String name, GoogleWorkspaceFileType type) {
        log.info("event=shared_file_google_file_create_시작 parentId={} type={}", parentId, type);
        if (name == null || name.isBlank()) {
            throw new SharedFileInvalidNameException();
        }
        if (parentId != null && parentId.isBlank()) {
            throw new BadRequestException();
        }
        if (type == null) {
            throw new BadRequestException();
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

        DriveItem created = sharedFileDrivePort.createWorkspaceFile(accessToken, targetParentId, name, type);
        SharedFileItemView result = SharedFileItemViewMapper.toView(created);
        log.info("event=shared_file_google_file_create_완료 itemId={} type={}", result.id(), type);
        return result;
    }
}
