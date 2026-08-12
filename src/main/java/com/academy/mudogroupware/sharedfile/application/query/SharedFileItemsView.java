package com.academy.mudogroupware.sharedfile.application.query;

import java.util.List;

// 목록·검색 페이지 응답. Drive의 nextPageToken을 nextCursor로 그대로 넘긴다.
public record SharedFileItemsView(List<SharedFileItemView> items, boolean hasNext, String nextCursor) {
}
