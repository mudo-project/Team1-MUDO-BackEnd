package com.academy.mudogroupware.sharedfile.application.port;

import java.util.List;

// 목록·검색 결과 페이지. Drive의 nextPageToken을 nextCursor로 그대로 넘긴다 — 총 개수·페이지 offset은 계산하지 않는다.
public record DrivePage(List<DriveItem> items, String nextCursor) {

    public boolean hasNext() {
        return nextCursor != null && !nextCursor.isBlank();
    }
}
