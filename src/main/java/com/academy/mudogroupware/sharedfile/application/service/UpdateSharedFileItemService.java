package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.UpdateSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidNameException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 이름 변경과 이동을 SharedFileDrivePort.updateItem() 호출 한 번으로 함께 반영한다. 예전에는
// RenameSharedFileItemService/MoveSharedFileItemService가 각각 별도 Drive 요청을 보내서, 이름 변경이
// 성공하고 이동이 실패하면 절반만 반영된 채로 남는 문제가 있었다(이슈 #406).
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateSharedFileItemService implements UpdateSharedFileItemUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    @Override
    public SharedFileItemView update(String itemId, String name, String newParentId) {
        boolean changingName = name != null && !name.isBlank();
        boolean changingParent = newParentId != null && !newParentId.isBlank();
        log.info("event=shared_file_update_시작 itemId={} changingName={} changingParent={}",
                itemId, changingName, changingParent);

        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        String rootId = root.getGoogleRootFolderId();

        sharedFileRootGuard.requireDescendant(accessToken, rootId, itemId);

        // 목적지 검증(순환·유형 확인)은 itemId 자신의 메타데이터가 필요 없으므로, 불필요한 Drive 조회를
        // 피하기 위해 itemId 조회보다 먼저 한다.
        if (changingParent) {
            validateDestination(accessToken, rootId, itemId, newParentId);
        }

        DriveItem current = sharedFileDrivePort.getItem(accessToken, itemId)
                .orElseThrow(() -> new SharedFileItemNotFoundException(itemId));

        if (changingName && current.isRegularFile() && !extensionOf(current.name()).equals(extensionOf(name))) {
            throw new SharedFileInvalidNameException();
        }

        String fromParentId = changingParent ? current.parentIds().get(0) : null;
        String toParentId = changingParent ? newParentId : null;

        DriveItem updated = sharedFileDrivePort.updateItem(
                accessToken, itemId, changingName ? name : null, fromParentId, toParentId);
        SharedFileItemView result = SharedFileItemViewMapper.toView(updated);
        log.info("event=shared_file_update_완료 itemId={} changingName={} changingParent={}",
                result.id(), changingName, changingParent);
        return result;
    }

    // 목적지가 폴더인지, 이동 대상 자신을 목적지로 지정했는지, 이동 대상의 자손을 목적지로 지정해 순환이
    // 생기는지 확인한다. 목적지가 시스템 루트 자신이면 생성·목록조회와 동일한 원칙으로 검증을 생략한다.
    private void validateDestination(String accessToken, String rootId, String itemId, String newParentId) {
        if (itemId.equals(newParentId)) {
            throw new SharedFileOutOfRootException(newParentId);
        }
        if (newParentId.equals(rootId)) {
            return;
        }
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

    private String extensionOf(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex < 0 ? "" : name.substring(dotIndex + 1);
    }
}
