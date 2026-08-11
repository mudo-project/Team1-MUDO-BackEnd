package com.academy.mudogroupware.sharedfile.application.port;

// Google 빈 파일 생성 시 선택 가능한 3종. 실제 Google MIME type 매핑은 GoogleDriveAdapter가 갖고 있어,
// 이 Port 계약 밖으로 Google 고유 문자열이 새어나가지 않게 한다.
public enum GoogleWorkspaceFileType {
    DOCS,
    SHEETS,
    SLIDES
}
