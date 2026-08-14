package com.academy.mudogroupware.sharedfile.application.query;

// 시스템 루트 상태 조회 응답. rootId는 ready일 때만 값이 있고, FAILED든 행이 아예 없든(연동 전) null이다
// — 생성/업로드 API에 parentId로 그대로 넘길 수 있도록 프론트에 노출한다.
public record SharedFileRootView(boolean ready, String rootId) {
}
