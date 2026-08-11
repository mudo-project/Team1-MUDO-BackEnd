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
}
