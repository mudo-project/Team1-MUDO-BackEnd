package com.academy.mudogroupware.sharedfile.application.query;

// 시스템 루트 상태 조회 응답. FAILED든 행이 아예 없든(연동 전) 프론트 입장에선 둘 다 "사용 불가"로
// 동일하게 취급하면 되므로 ready 단일 boolean만 노출한다.
public record SharedFileRootView(boolean ready) {
}
