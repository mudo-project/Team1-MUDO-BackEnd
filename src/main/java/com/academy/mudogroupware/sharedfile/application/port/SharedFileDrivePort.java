package com.academy.mudogroupware.sharedfile.application.port;

import java.util.Optional;

// 공유파일 애플리케이션 서비스가 Google Drive를 호출하는 유일한 경계. GoogleDriveAdapter가 구현하며,
// URL·검색 쿼리·MIME type 등 Drive API의 세부사항은 이 인터페이스 사용자에게 노출하지 않는다.
public interface SharedFileDrivePort {

    // 단일 항목 메타데이터 조회. 404면 Optional.empty(), 401/403/5xx/네트워크 오류면 예외를 던진다
    // (SharedFileRootGuard가 경로 추적에, 다운로드·export가 파일명 조회에 사용).
    Optional<DriveItem> getItem(String accessToken, String itemId);

    // 지정 폴더의 직계 하위 폴더·파일 목록(cursor 기반 페이지네이션).
    DrivePage listChildren(String accessToken, String parentId, String cursor, int size);

    // 이름으로 전체 검색. 시스템 루트 하위인지 여부는 호출자가 SharedFileRootGuard로 별도 검증한다.
    DrivePage searchByName(String accessToken, String keyword, String cursor, int size);

    // 배포 단위의 시스템 루트 자체를 최상위(부모 없이)에 생성한다. Google 연결 성공 이벤트로 1회 호출된다.
    DriveItem createRootFolder(String accessToken, String name);

    // 시스템 루트 하위에 일반 폴더를 생성한다.
    DriveItem createFolder(String accessToken, String parentId, String name);

    // 로컬 파일을 지정 폴더에 업로드한다. (Task4에서 구현 예정)
    DriveItem upload(String accessToken, String parentId, String name, String contentType, byte[] content);

    // Docs/Sheets/Slides 중 하나의 빈 파일을 생성한다.
    DriveItem createWorkspaceFile(String accessToken, String parentId, String name, String workspaceMimeType);

    // 이름만 변경한다(확장자 유지 여부는 UseCase 책임).
    DriveItem rename(String accessToken, String itemId, String name);

    // 같은 시스템 루트 내부의 다른 폴더로 부모를 교체한다.
    DriveItem move(String accessToken, String itemId, String fromParentId, String toParentId);

    // Drive 휴지통으로 이동한다(영구 삭제 아님).
    void trash(String accessToken, String itemId);

    // 직접 업로드한 원본 파일을 그대로 내려받는다.
    DriveBinary downloadOriginal(String accessToken, String itemId);

    // Google Docs·Sheets·Slides를 PDF/DOCX·PDF/XLSX·PDF/PPTX 중 하나로 변환해 내려받는다.
    DriveBinary export(String accessToken, String itemId, GoogleWorkspaceExportFormat format);
}
