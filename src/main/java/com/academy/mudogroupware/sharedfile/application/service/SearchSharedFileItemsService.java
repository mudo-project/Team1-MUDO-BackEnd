package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;
import com.academy.mudogroupware.sharedfile.application.usecase.SearchSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchSharedFileItemsService implements SearchSharedFileItemsUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // Drive의 이름 검색은 시스템 루트 밖의 결과와 다른 유형도 함께 돌려줄 수 있어, type으로 먼저 좁히고
    // 남은 후보마다 Guard로 걸러낸 뒤 루트 하위인 것만 응답에 담는다.
    @Override
    public SharedFileItemsView search(String keyword, SharedFileItemType type, String cursor, int size) {
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();

        DrivePage page = sharedFileDrivePort.searchByName(accessToken, keyword, cursor, size);
        var items = page.items().stream()
                .filter(candidate -> matchesType(candidate, type))
                .filter(candidate -> isUnderRoot(accessToken, root.getGoogleRootFolderId(), candidate))
                .map(SharedFileItemViewMapper::toView)
                .toList();
        return new SharedFileItemsView(items, page.hasNext(), page.nextCursor());
    }

    private boolean matchesType(DriveItem candidate, SharedFileItemType type) {
        if (type == null) {
            return true;
        }
        return type == SharedFileItemType.FOLDER ? candidate.isFolder() : !candidate.isFolder();
    }

    private boolean isUnderRoot(String accessToken, String rootId, DriveItem candidate) {
        try {
            sharedFileRootGuard.requireDescendant(accessToken, rootId, candidate.id());
            return true;
        } catch (SharedFileOutOfRootException e) {
            return false;
        }
    }
}
