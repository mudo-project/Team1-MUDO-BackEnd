package com.academy.mudogroupware.sharedfile.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.google.application.usecase.GetGoogleAccessTokenUseCase;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemViewMapper;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;
import com.academy.mudogroupware.sharedfile.application.usecase.SearchSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileRootUnavailableException;
import com.academy.mudogroupware.sharedfile.domain.model.SharedFileRoot;
import com.academy.mudogroupware.sharedfile.domain.repository.SharedFileRootRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchSharedFileItemsService implements SearchSharedFileItemsUseCase {

    private final SharedFileRootRepository sharedFileRootRepository;
    private final SharedFileRootGuard sharedFileRootGuard;
    private final SharedFileDrivePort sharedFileDrivePort;
    private final GetGoogleAccessTokenUseCase getGoogleAccessTokenUseCase;

    // Drive의 이름 검색은 시스템 루트 밖의 결과와 다른 유형도 함께 돌려줄 수 있어, type으로 먼저 좁히고
    // 남은 후보마다 Guard로 걸러낸 뒤 루트 하위인 것만 응답에 담는다. 한 원본 페이지를 필터링한 결과가
    // size보다 적으면(예: 그 페이지가 전부 루트 밖) 원본 페이지가 남아있는 한 계속 더 가져온다 — 그렇지
    // 않으면 필터링 전 크기만 보고 실제로는 결과가 더 있는데도 적게, 심지어 0건으로 응답할 수 있다.
    @Override
    public SharedFileItemsView search(String keyword, SharedFileItemType type, String cursor, int size) {
        log.info("event=shared_file_search_시작 type={} cursorPresent={} size={}",
                type, cursor != null && !cursor.isBlank(), size);
        SharedFileRoot root = sharedFileRootRepository.find()
                .filter(SharedFileRoot::isReady)
                .orElseThrow(SharedFileRootUnavailableException::new);
        String accessToken = getGoogleAccessTokenUseCase.getAccessToken();
        String rootId = root.getGoogleRootFolderId();

        List<SharedFileItemView> matches = new ArrayList<>();
        String rawCursor = cursor;
        String rawNextCursor = null;
        boolean rawHasNext = false;
        while (true) {
            DrivePage page = sharedFileDrivePort.searchByName(accessToken, keyword, rawCursor, size);
            for (DriveItem candidate : page.items()) {
                if (matchesType(candidate, type) && isUnderRoot(accessToken, rootId, candidate)) {
                    matches.add(SharedFileItemViewMapper.toView(candidate));
                }
            }
            rawNextCursor = page.nextCursor();
            rawHasNext = page.hasNext();
            if (matches.size() >= size || !rawHasNext) {
                break;
            }
            rawCursor = rawNextCursor;
        }

        // 마지막으로 처리한 원본 페이지 안에서 size를 넘겨 남는 매칭이 생기면, 그 남는 것들은 다음 커서로는
        // 다시 가져올 수 없다(원본 커서는 페이지 단위라 페이지 중간을 가리키지 못함) — hasNext만 true로 알리고 버린다.
        boolean overflowedWithinLastPage = matches.size() > size;
        List<SharedFileItemView> items = overflowedWithinLastPage ? matches.subList(0, size) : matches;
        boolean hasNext = rawHasNext || overflowedWithinLastPage;
        SharedFileItemsView result = new SharedFileItemsView(items, hasNext, rawNextCursor);
        log.info("event=shared_file_search_완료 type={} itemCount={} hasNext={}",
                type, result.items().size(), result.hasNext());
        return result;
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
