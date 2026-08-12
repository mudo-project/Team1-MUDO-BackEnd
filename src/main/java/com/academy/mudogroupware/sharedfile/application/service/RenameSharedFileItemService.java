package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.usecase.RenameSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileInvalidNameException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RenameSharedFileItemService implements RenameSharedFileItemUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // 일반 업로드 파일은 확장자가 바뀌면 내용과 이름이 어긋나므로 거부한다. 폴더·Google 파일은
    // 확장자 개념이 없어 이름을 그대로 받는다.
    @Override
    public SharedFileItemView rename(String itemId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new SharedFileInvalidNameException();
        }
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();

        sharedFileRootGuard.requireDescendant(accessToken, root.getGoogleRootFolderId(), itemId);

        DriveItem current = sharedFileDrivePort.getItem(accessToken, itemId)
                .orElseThrow(() -> new SharedFileItemNotFoundException(itemId));
        if (current.isRegularFile() && !extensionOf(current.name()).equals(extensionOf(newName))) {
            throw new SharedFileInvalidNameException();
        }

        DriveItem renamed = sharedFileDrivePort.rename(accessToken, itemId, newName);
        return SharedFileItemViewMapper.toView(renamed);
    }

    private String extensionOf(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex < 0 ? "" : name.substring(dotIndex + 1);
    }
}
