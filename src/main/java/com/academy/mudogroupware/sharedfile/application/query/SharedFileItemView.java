package com.academy.mudogroupware.sharedfile.application.query;

import java.time.LocalDateTime;

// 목록·검색·상세 조회가 공통으로 쓰는 항목 응답. parentIds 같은 내부 경로 정보는 API 응답에 노출하지 않는다.
public record SharedFileItemView(
        String id,
        String name,
        String mimeType,
        String viewUrl,
        boolean downloadable,
        LocalDateTime modifiedAt
) {
}
