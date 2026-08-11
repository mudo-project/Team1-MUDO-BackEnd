package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.MoveSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MoveSharedFileItemService implements MoveSharedFileItemUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // PATCH 요청은 새 parentId만 받으므로, 현재 parentId(removeParents용)는 Drive에서 직접 조회한다.
    @Override
    public SharedFileItemView move(String itemId, String newParentId) {
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        String rootId = root.getGoogleRootFolderId();

        sharedFileRootGuard.requireDescendant(accessToken, rootId, itemId);
        if (!newParentId.equals(rootId)) {
            sharedFileRootGuard.requireDescendant(accessToken, rootId, newParentId);
        }

        DriveItem current = sharedFileDrivePort.getItem(accessToken, itemId)
                .orElseThrow(() -> new SharedFileItemNotFoundException(itemId));
        String oldParentId = current.parentIds().get(0);

        DriveItem moved = sharedFileDrivePort.move(accessToken, itemId, oldParentId, newParentId);
        return SharedFileItemViewMapper.toView(moved);
    }
}
