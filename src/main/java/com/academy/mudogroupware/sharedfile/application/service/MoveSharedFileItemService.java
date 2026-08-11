package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.MoveSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
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
    // 목적지가 폴더인지, 그리고 이동 대상 자신의 하위로 옮기는 순환이 아닌지도 함께 확인한다 —
    // 그렇지 않으면 파일을 목적지로 지정하거나 폴더를 자기 자손 폴더로 옮겨 구조가 깨질 수 있다.
    @Override
    public SharedFileItemView move(String itemId, String newParentId) {
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        String rootId = root.getGoogleRootFolderId();

        sharedFileRootGuard.requireDescendant(accessToken, rootId, itemId);
        if (itemId.equals(newParentId)) {
            throw new SharedFileOutOfRootException(newParentId);
        }

        if (!newParentId.equals(rootId)) {
            sharedFileRootGuard.requireDescendant(accessToken, rootId, newParentId);
            DriveItem destination = sharedFileDrivePort.getItem(accessToken, newParentId)
                    .orElseThrow(() -> new SharedFileItemNotFoundException(newParentId));
            if (!destination.isFolder()) {
                throw new SharedFileOutOfRootException(newParentId);
            }
            if (createsCycle(accessToken, destination, itemId, rootId)) {
                throw new SharedFileOutOfRootException(newParentId);
            }
        }

        DriveItem current = sharedFileDrivePort.getItem(accessToken, itemId)
                .orElseThrow(() -> new SharedFileItemNotFoundException(itemId));
        String oldParentId = current.parentIds().get(0);

        DriveItem moved = sharedFileDrivePort.move(accessToken, itemId, oldParentId, newParentId);
        return SharedFileItemViewMapper.toView(moved);
    }

    // destination의 부모를 따라 시스템 루트까지 올라가며 itemId를 만나는지 확인한다. 만나면 itemId를
    // 자기 자손인 destination으로 옮기는 순환 이동이라는 뜻이다. 루트에 도달하면(itemId는 루트일 수 없으므로) 순환이 아니다.
    private boolean createsCycle(String accessToken, DriveItem destination, String itemId, String rootId) {
        DriveItem current = destination;
        while (!current.parentIds().isEmpty()) {
            String parentId = current.parentIds().get(0);
            if (parentId.equals(itemId)) {
                return true;
            }
            if (parentId.equals(rootId)) {
                return false;
            }
            current = sharedFileDrivePort.getItem(accessToken, parentId)
                    .orElseThrow(() -> new SharedFileItemNotFoundException(parentId));
        }
        return false;
    }
}
