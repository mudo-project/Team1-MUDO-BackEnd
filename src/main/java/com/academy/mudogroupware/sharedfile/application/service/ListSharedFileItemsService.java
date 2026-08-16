package com.academy.mudogroupware.sharedfile.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;
import com.academy.mudogroupware.sharedfile.application.usecase.ListSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ListSharedFileItemsService implements ListSharedFileItemsUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // parentId가 없으면 시스템 루트 자신을 조회 대상으로 삼는다. 이 경우엔 루트 자체이므로 Guard 검증이
    // 필요 없고, 값이 주어지면 루트 하위인지 반드시 확인한 뒤에만 Drive를 호출한다.
    @Override
    public SharedFileItemsView list(String parentId, String cursor, int size) {
        log.info("event=shared_file_list_시작 parentId={} cursorPresent={} size={}",
                parentId, cursor != null && !cursor.isBlank(), size);
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String rootId = root.getGoogleRootFolderId();
        String targetParentId = parentId == null ? rootId : parentId;

        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        if (!targetParentId.equals(rootId)) {
            sharedFileRootGuard.requireDescendant(accessToken, rootId, targetParentId);
        }

        DrivePage page = sharedFileDrivePort.listChildren(accessToken, targetParentId, cursor, size);
        SharedFileItemsView result = new SharedFileItemsView(
                page.items().stream().map(SharedFileItemViewMapper::toView).toList(),
                page.hasNext(), page.nextCursor());
        log.info("event=shared_file_list_완료 parentId={} itemCount={} hasNext={}",
                targetParentId, result.items().size(), result.hasNext());
        return result;
    }
}
