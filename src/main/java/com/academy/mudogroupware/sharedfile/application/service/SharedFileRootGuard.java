package com.academy.mudogroupware.sharedfile.application.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileOutOfRootException;

import lombok.RequiredArgsConstructor;

// 클라이언트가 전달한 itemId를 그대로 믿지 않고, Drive의 parentIds를 실제로 따라 올라가며
// 시스템 루트 하위인지 검증한다. 로컬에 폴더·파일 트리를 복제해두지 않기 때문에 매 요청마다 이 방식으로 확인한다.
@Component
@RequiredArgsConstructor
public class SharedFileRootGuard {

    private final SharedFileDrivePort drivePort;

    // itemId가 rootId의 자손(직계·중첩 모두)인지 확인한다. 시스템 루트 자체를 대상으로 지정하거나,
    // 부모를 따라가다 루트에 도달하지 못하고 끝나면(부모 없음) 거부한다.
    public void requireDescendant(String accessToken, String rootId, String itemId) {
        if (itemId.equals(rootId)) {
            throw new SharedFileOutOfRootException(itemId);
        }

        String currentId = itemId;
        while (true) {
            Optional<DriveItem> found = drivePort.getItem(accessToken, currentId);
            if (found.isEmpty()) {
                throw new SharedFileItemNotFoundException(currentId);
            }

            DriveItem item = found.get();
            if (item.parentIds().contains(rootId)) {
                return;
            }
            if (item.parentIds().isEmpty()) {
                throw new SharedFileOutOfRootException(itemId);
            }
            // 아직 루트에 닿지 않았으면 한 단계 위 부모로 올라가 계속 확인한다.
            currentId = item.parentIds().get(0);
        }
    }
}
