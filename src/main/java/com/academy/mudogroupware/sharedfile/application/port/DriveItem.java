package com.academy.mudogroupware.sharedfile.application.port;

import java.time.LocalDateTime;
import java.util.List;

// Drive의 파일·폴더 메타데이터를 감싼 값 객체. parentIds는 SharedFileRootGuard가 루트까지 경로를
// 추적하는 데 쓰이고, downloadable은 Drive가 미리보기를 지원하지 않는 형식을 프론트에 안내할 때 쓰인다.
public record DriveItem(
        String id,
        String name,
        String mimeType,
        List<String> parentIds,
        String viewUrl,
        boolean downloadable,
        LocalDateTime modifiedAt,
        boolean trashed
) {
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    // parentIds를 불변 스냅샷으로 저장한다. Jackson이 만드는 List는 가변이라, 방어적 복사가
    // 없으면 나중에 외부에서 원본이 변경될 때 SharedFileRootGuard의 경로 판정이 흔들릴 수 있다.
    public DriveItem {
        parentIds = List.copyOf(parentIds);
    }

    public boolean isFolder() {
        return FOLDER_MIME_TYPE.equals(mimeType);
    }
}
